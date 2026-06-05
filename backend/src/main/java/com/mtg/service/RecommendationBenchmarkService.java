package com.mtg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtg.dto.RecommendationBenchmarkCoverageDTO;
import com.mtg.dto.RecommendationBenchmarkMetricDTO;
import com.mtg.dto.RecommendationBenchmarkNextActionDTO;
import com.mtg.dto.RecommendationBenchmarkReviewCaseDTO;
import com.mtg.dto.RecommendationBenchmarkReviewRequestDTO;
import com.mtg.dto.RecommendationBenchmarkSummaryDTO;
import com.mtg.model.RecommendationAuditRun;
import com.mtg.model.RecommendationBenchmarkCaseResult;
import com.mtg.model.RecommendationBenchmarkReview;
import com.mtg.model.RecommendationBenchmarkRun;
import com.mtg.repository.RecommendationAuditRepository;
import com.mtg.repository.RecommendationBenchmarkCaseRepository;
import com.mtg.repository.RecommendationBenchmarkReviewRepository;
import com.mtg.repository.RecommendationBenchmarkRunRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@ApplicationScoped
public class RecommendationBenchmarkService {
    private static final Logger LOG = Logger.getLogger(RecommendationBenchmarkService.class);
    private static final String RESOURCE = "recommendation-benchmark/cases-v1.json";
    private static final int TARGET_CASES = 50;
    private static final int REVIEW_QUORUM = 3;
    private static final AtomicBoolean RUNNING = new AtomicBoolean();

    @Inject ObjectMapper objectMapper;
    @Inject RecommendationAuditRepository auditRepository;
    @Inject RecommendationBenchmarkRunRepository runRepository;
    @Inject RecommendationBenchmarkCaseRepository caseRepository;
    @Inject RecommendationBenchmarkReviewRepository reviewRepository;
    @Inject RecommendationBenchmarkAiService aiService;
    @Inject RecommendationBenchmarkScenarioService scenarioService;

    @Transactional
    public RecommendationBenchmarkSummaryDTO run() {
        if (!RUNNING.compareAndSet(false, true) || runRepository.hasRunning()) {
            throw new IllegalStateException("benchmark_already_running");
        }
        long startedNanos = System.nanoTime();
        RecommendationBenchmarkRun run = new RecommendationBenchmarkRun();
        try {
            JsonNode root = fixtureRoot();
            JsonNode cases = root.path("cases");
            run.setStatus("running");
            run.setFixtureVersion(root.path("fixtureVersion").asText("unknown"));
            run.setBaselineVersion(root.path("baselineVersion").asText("unknown"));
            run.setAlgorithmVersion(root.path("algorithmVersion").asText("unknown"));
            run.setStartedAt(OffsetDateTime.now());
            run.setTotalCases(cases.size());
            run.setEvaluatedCases(0);
            runRepository.persistAndFlush(run);
            LOG.infov("event=benchmark.run.started runId={0} fixtureVersion={1} cases={2}", run.getId(), run.getFixtureVersion(), cases.size());

            Totals totals = new Totals();
            for (JsonNode fixture : cases) {
                CaseMetrics metrics = evaluate(fixture, totals);
                caseRepository.persist(toCaseResult(run.getId(), fixture, metrics));
                run.setEvaluatedCases(run.getEvaluatedCases() + 1);
            }
            run.setMetricsJson(toJson(metrics(totals, cases.size(), 0, 0)));
            run.setStatus("success");
            run.setFinishedAt(OffsetDateTime.now());
            LOG.infov(
                    "event=benchmark.run.completed runId={0} cases={1} durationMs={2} violations={3}",
                    run.getId(), cases.size(), (System.nanoTime() - startedNanos) / 1_000_000, totals.violations()
            );
            return summary();
        } catch (RuntimeException exception) {
            LOG.errorv(exception, "event=benchmark.run.failed runId={0} errorCode=benchmark_execution_failed", run.getId());
            throw exception;
        } finally {
            RUNNING.set(false);
        }
    }

    public RecommendationBenchmarkSummaryDTO summary() {
        JsonNode root = fixtureRoot();
        List<RecommendationBenchmarkCoverageDTO> coverage = coverage(root.path("cases"));
        RecommendationBenchmarkRun latest = runRepository.latestSuccessful();
        List<RecommendationBenchmarkMetricDTO> calculated = latest == null
                ? pendingMetrics()
                : readMetrics(latest.getMetricsJson());
        Map<String, Object> reviewProgress = reviewProgress(latest);
        if (latest != null) {
            calculated = withBlindMetric(calculated, reviewProgress);
        }
        Map<String, Object> feedbackBreakdown = feedbackBreakdown();
        int totalCases = root.path("cases").size();
        int reviewedCases = ((Number) reviewProgress.getOrDefault("completedCases", 0)).intValue();
        boolean objectiveReady = latest != null
                && latest.getEvaluatedCases() >= TARGET_CASES
                && metricReady(calculated, "commanderLegalityPassRate")
                && metricReady(calculated, "actionabilityRate")
                && metricReady(calculated, "preferenceAdherenceRate");
        boolean automaticReady = totalCases >= TARGET_CASES
                && objectiveReady
                && aiService != null
                && aiService.hasCurrentQualifiedSet(totalCases);
        RecommendationBenchmarkSummaryDTO result = new RecommendationBenchmarkSummaryDTO(
                automaticReady ? "automatic_benchmark_ready" : "automatic_benchmark_in_progress",
                "gpt_5_5_artifacts_required_human_validation_pending",
                totalCases,
                TARGET_CASES,
                coverage,
                calculated,
                List.of(
                        "A vantagem automatica so pode ser comunicada como qualificada e ainda nao possui validacao humana.",
                        "Feedback, julgamentos e revisao humana orientam investigacao; pesos nunca mudam automaticamente."
                ),
                latest == null ? 0 : latest.getEvaluatedCases(),
                reviewedCases,
                feedbackCounts(),
                nextActions(totalCases, reviewedCases, feedbackCounts())
        );
        result.setLastRunId(latest == null ? null : latest.getId());
        result.setLastRunAt(latest == null ? null : latest.getFinishedAt());
        result.setReviewProgress(reviewProgress);
        result.setFeedbackBreakdown(feedbackBreakdown);
        result.setAiArtifacts(aiService == null ? Map.of() : aiService.statusSummary());
        Map<String, Object> corpusStatus = corpusStatus(root);
        result.setCorpusStatus(corpusStatus);
        result.setPipeline(pipeline(corpusStatus, latest, result.getAiArtifacts()));
        return result;
    }

    public RecommendationBenchmarkReviewCaseDTO nextReview(String reviewerId) {
        RecommendationBenchmarkRun latest = requireLatest();
        Map<String, Long> counts = reviewRepository.byRun(latest.getId()).stream()
                .collect(Collectors.groupingBy(RecommendationBenchmarkReview::getCaseId, Collectors.counting()));
        RecommendationBenchmarkCaseResult selected = caseRepository.byRun(latest.getId()).stream()
                .filter(item -> counts.getOrDefault(item.getCaseId(), 0L) < REVIEW_QUORUM)
                .filter(item -> reviewRepository.byReviewer(latest.getId(), item.getCaseId(), reviewerId) == null)
                .findFirst()
                .orElse(null);
        if (selected == null) return null;
        return new RecommendationBenchmarkReviewCaseDTO(
                latest.getId(),
                selected.getCaseId(),
                selected.getCommander(),
                selected.getBracket(),
                readList(selected.getOptionAJson()),
                readList(selected.getOptionBJson()),
                counts.getOrDefault(selected.getCaseId(), 0L).intValue(),
                REVIEW_QUORUM
        );
    }

    @Transactional
    public void review(String caseId, RecommendationBenchmarkReviewRequestDTO request, String reviewerId) {
        if (request == null || request.runId() == null || !Set.of("A", "B", "tie").contains(request.choice())) {
            throw new IllegalArgumentException("runId and choice A, B or tie are required");
        }
        RecommendationBenchmarkCaseResult benchmarkCase = caseRepository.byRunAndCase(request.runId(), caseId);
        if (benchmarkCase == null) throw new NotFoundException("Benchmark case not found");
        RecommendationBenchmarkReview review = reviewRepository.byReviewer(request.runId(), caseId, reviewerId);
        OffsetDateTime now = OffsetDateTime.now();
        if (review == null) {
            review = new RecommendationBenchmarkReview();
            review.setRunId(request.runId());
            review.setCaseId(caseId);
            review.setReviewerId(reviewerId);
            review.setCreatedAt(now);
            review.setChoice(request.choice());
            review.setUpdatedAt(now);
            reviewRepository.persist(review);
        } else {
            review.setChoice(request.choice());
            review.setUpdatedAt(now);
        }
        LOG.infov("event=benchmark.review.recorded runId={0} caseId={1} choice={2}", request.runId(), caseId, request.choice());
    }

    private CaseMetrics evaluate(JsonNode fixture, Totals totals) {
        Set<String> expectedAdds = textSet(fixture.path("labels").path("expectedAdds"));
        Set<String> expectedCuts = textSet(fixture.path("labels").path("expectedCuts"));
        Set<String> protectedCards = textSet(fixture.path("labels").path("protectedCards"));
        JsonNode system = scenarioService.execute(fixture).output();
        RecommendationBenchmarkScenarioService.Validation validation = scenarioService.validate(fixture, system);
        int addHits = 0;
        int cutHits = 0;
        int actionable = 0;
        int violations = 0;
        for (JsonNode item : system) {
            if (expectedAdds.contains(normalize(item.path("add").asText()))) addHits++;
            if (expectedCuts.contains(normalize(item.path("remove").asText()))) cutHits++;
            boolean complete = hasText(item, "add") && hasText(item, "remove") && hasText(item, "reasoning") && hasText(item, "risk");
            if (complete) actionable++;
        }
        violations += validation.violations().size();
        RecommendationBenchmarkScenarioService.PreferenceCounts preferences = scenarioService.preferenceCounts(fixture, system);
        int declared = preferences.declared();
        int met = preferences.met();
        totals.addHits += addHits;
        totals.addCount += system.size();
        totals.cutHits += cutHits;
        totals.cutCount += system.size();
        totals.actionable += actionable;
        totals.recommendations += system.size();
        totals.preferencesMet += met;
        totals.preferencesDeclared += declared;
        totals.violations += violations;
        return new CaseMetrics(addHits, system.size(), cutHits, system.size(), actionable, system.size(), met, declared, violations);
    }

    private RecommendationBenchmarkCaseResult toCaseResult(Long runId, JsonNode fixture, CaseMetrics metrics) {
        RecommendationBenchmarkCaseResult result = new RecommendationBenchmarkCaseResult();
        result.setRunId(runId);
        result.setCaseId(fixture.path("id").asText());
        result.setCommander(fixture.path("commander").asText());
        result.setBracket(fixture.path("bracket").asText());
        String system = toJson(scenarioService.execute(fixture).output());
        String gpt = toJson(fixture.path("gpt"));
        boolean systemIsA = Math.floorMod(result.getCaseId().hashCode(), 2) == 0;
        result.setSystemOutputJson(system);
        result.setGptOutputJson(gpt);
        result.setOptionAJson(systemIsA ? system : gpt);
        result.setOptionBJson(systemIsA ? gpt : system);
        result.setSystemOption(systemIsA ? "A" : "B");
        result.setMetricsJson(toJson(metrics));
        return result;
    }

    private List<RecommendationBenchmarkMetricDTO> metrics(Totals totals, int cases, int systemWins, int humanCases) {
        return List.of(
                metric("commanderLegalityPassRate", totals.violations == 0 ? 1 : 0, 1.0, cases),
                metric("offColorDuplicateProtectedViolationRate", totals.recommendations == 0 ? 0 : totals.violations / (double) totals.recommendations, 0.0, totals.recommendations),
                metric("addPrecisionAt10", ratio(totals.addHits, totals.addCount), 0.70, totals.addCount),
                metric("cutPrecisionAt10", ratio(totals.cutHits, totals.cutCount), 0.70, totals.cutCount),
                metric("preferenceAdherenceRate", ratio(totals.preferencesMet, totals.preferencesDeclared), 0.80, totals.preferencesDeclared),
                metric("actionabilityRate", ratio(totals.actionable, totals.recommendations), 0.90, totals.recommendations),
                humanCases == 0
                        ? new RecommendationBenchmarkMetricDTO("blindPreferenceWinRate", "pending_human_review", ">= 0.60", "not_ready", 0)
                        : metric("blindPreferenceWinRate", ratio(systemWins, humanCases), 0.60, humanCases)
        );
    }

    private RecommendationBenchmarkMetricDTO metric(String name, double value, double target, int sample) {
        boolean lowerIsBetter = name.contains("Violation");
        boolean ready = lowerIsBetter ? value <= target : value >= target;
        return new RecommendationBenchmarkMetricDTO(name, String.format(Locale.ROOT, "%.1f%%", value * 100), (lowerIsBetter ? "<= " : ">= ") + String.format(Locale.ROOT, "%.0f%%", target * 100), ready ? "ready" : "needs_attention", sample);
    }

    private List<RecommendationBenchmarkMetricDTO> pendingMetrics() {
        return List.of(
                new RecommendationBenchmarkMetricDTO("addPrecisionAt10", "pending_first_run", ">= 70%", "not_ready"),
                new RecommendationBenchmarkMetricDTO("cutPrecisionAt10", "pending_first_run", ">= 70%", "not_ready"),
                new RecommendationBenchmarkMetricDTO("preferenceAdherenceRate", "pending_first_run", ">= 80%", "not_ready"),
                new RecommendationBenchmarkMetricDTO("actionabilityRate", "pending_first_run", ">= 90%", "not_ready"),
                new RecommendationBenchmarkMetricDTO("blindPreferenceWinRate", "pending_human_review", ">= 60%", "not_ready")
        );
    }

    private List<RecommendationBenchmarkMetricDTO> withBlindMetric(List<RecommendationBenchmarkMetricDTO> metrics, Map<String, Object> progress) {
        int completed = ((Number) progress.getOrDefault("completedCases", 0)).intValue();
        int systemWins = ((Number) progress.getOrDefault("systemWins", 0)).intValue();
        List<RecommendationBenchmarkMetricDTO> updated = new ArrayList<>(metrics.stream()
                .filter(metric -> !"blindPreferenceWinRate".equals(metric.getName()))
                .toList());
        updated.add(completed == 0
                ? new RecommendationBenchmarkMetricDTO("blindPreferenceWinRate", "pending_human_review", ">= 60%", "not_ready", 0)
                : metric("blindPreferenceWinRate", ratio(systemWins, completed), 0.60, completed));
        return List.copyOf(updated);
    }

    private Map<String, Object> reviewProgress(RecommendationBenchmarkRun latest) {
        if (latest == null) return Map.of("completedCases", 0, "totalCases", 0, "votes", 0, "requiredVotes", 0, "systemWins", 0, "gptWins", 0, "ties", 0);
        List<RecommendationBenchmarkReview> reviews = reviewRepository.byRun(latest.getId());
        Map<String, List<RecommendationBenchmarkReview>> byCase = reviews.stream().collect(Collectors.groupingBy(RecommendationBenchmarkReview::getCaseId));
        int completed = 0;
        int systemWins = 0;
        int gptWins = 0;
        int ties = 0;
        for (RecommendationBenchmarkCaseResult item : caseRepository.byRun(latest.getId())) {
            List<RecommendationBenchmarkReview> votes = byCase.getOrDefault(item.getCaseId(), List.of());
            if (votes.size() < REVIEW_QUORUM) continue;
            completed++;
            long system = votes.stream().filter(vote -> item.getSystemOption().equals(vote.getChoice())).count();
            long gpt = votes.stream().filter(vote -> !"tie".equals(vote.getChoice()) && !item.getSystemOption().equals(vote.getChoice())).count();
            if (system > gpt) systemWins++; else if (gpt > system) gptWins++; else ties++;
        }
        return Map.of("completedCases", completed, "totalCases", latest.getTotalCases(), "votes", reviews.size(), "requiredVotes", latest.getTotalCases() * REVIEW_QUORUM, "systemWins", systemWins, "gptWins", gptWins, "ties", ties);
    }

    private Map<String, Object> feedbackBreakdown() {
        List<RecommendationAuditRun> audits = auditRepository.listAll();
        Map<String, Long> byCommander = audits.stream().filter(item -> item.getFeedbackStatus() != null && item.getCommander() != null)
                .collect(Collectors.groupingBy(RecommendationAuditRun::getCommander, Collectors.counting()));
        Map<String, Long> byBracket = audits.stream().filter(item -> item.getFeedbackStatus() != null && item.getBracket() != null)
                .collect(Collectors.groupingBy(RecommendationAuditRun::getBracket, Collectors.counting()));
        Map<String, Long> byReason = audits.stream().filter(item -> item.getFeedbackStatus() != null && item.getFeedbackReason() != null)
                .collect(Collectors.groupingBy(RecommendationAuditRun::getFeedbackReason, Collectors.counting()));
        LOG.debugv("event=benchmark.feedback.aggregated audits={0} commanders={1} brackets={2}", audits.size(), byCommander.size(), byBracket.size());
        return Map.of("byCommander", byCommander, "byBracket", byBracket, "byReason", byReason);
    }

    private Map<String, Long> feedbackCounts() {
        return Map.of("accepted", feedbackCount("accepted"), "rejected", feedbackCount("rejected"), "needsReview", feedbackCount("needs_review"));
    }

    private List<RecommendationBenchmarkNextActionDTO> nextActions(int totalCases, int reviewedCases, Map<String, Long> feedback) {
        long feedbackTotal = feedback.values().stream().mapToLong(Long::longValue).sum();
        return List.of(
                new RecommendationBenchmarkNextActionDTO("expand-corpus", "Expandir corpus versionado", totalCases >= TARGET_CASES ? "ready" : "in_progress", "maintainer", "Ampliar de 20 para pelo menos 50 casos completos.", totalCases, TARGET_CASES, "documentation"),
                new RecommendationBenchmarkNextActionDTO("generate-ai-artifacts", "Gerar comparações GPT-5.5", aiService != null && aiService.hasCurrentQualifiedSet(totalCases) ? "ready" : "pending", "maintainer", "Gerar duas referências e três julgamentos cegos por comparação.", 0, totalCases, "automatic_benchmark"),
                new RecommendationBenchmarkNextActionDTO("human-review", "Validação humana posterior", reviewedCases >= totalCases ? "ready" : "deferred", "reviewer", "Obter três avaliações independentes por caso sem bloquear a fase automática.", reviewedCases, totalCases, "human_review"),
                new RecommendationBenchmarkNextActionDTO("collect-feedback", "Coletar feedback de recomendações", feedbackTotal > 0 ? "in_progress" : "pending", "user", "Acompanhar utilidade sem ajustar pesos automaticamente.", Math.toIntExact(feedbackTotal), null, "product_feedback")
        );
    }

    private boolean metricReady(List<RecommendationBenchmarkMetricDTO> metrics, String name) {
        return metrics.stream().anyMatch(metric -> name.equals(metric.getName()) && "ready".equals(metric.getStatus()));
    }

    private List<RecommendationBenchmarkCoverageDTO> coverage(JsonNode cases) {
        Map<String, List<JsonNode>> groups = new LinkedHashMap<>();
        cases.forEach(item -> groups.computeIfAbsent(item.path("commander").asText() + "|" + item.path("bracket").asText(), key -> new ArrayList<>()).add(item));
        return groups.entrySet().stream().map(entry -> {
            String[] key = entry.getKey().split("\\|", 2);
            return new RecommendationBenchmarkCoverageDTO(key[0], key[1], entry.getValue().size(), "curated_reference");
        }).toList();
    }

    private Map<String, Object> corpusStatus(JsonNode root) {
        int valid = 0;
        int archidekt = 0;
        int topDeck = 0;
        java.util.Set<String> commanders = new java.util.HashSet<>();
        for (JsonNode fixture : root.path("cases")) {
            if (scenarioService.validateFixture(fixture).isEmpty() && commanders.add(normalize(fixture.path("commander").asText()))) valid++;
            if ("archidekt_popular".equals(fixture.path("source").asText())) archidekt++;
            if ("topdeck_tournament".equals(fixture.path("source").asText())) topDeck++;
        }
        int candidates = archidektCandidateCount();
        List<String> blockers = new ArrayList<>();
        if (valid < TARGET_CASES) blockers.add("Faltam capturas completas e auditáveis para atingir 50 casos válidos.");
        if (topDeck < 25) blockers.add("A parcela competitiva TopDeck.gg ainda não possui 25 casos congelados.");
        return Map.of(
                "candidatesFound", candidates,
                "snapshotsComplete", archidekt + topDeck,
                "validCases", valid,
                "targetCases", TARGET_CASES,
                "archidektCases", archidekt,
                "topDeckCases", topDeck,
                "blockers", blockers
        );
    }

    private List<Map<String, Object>> pipeline(Map<String, Object> corpus, RecommendationBenchmarkRun latest, Map<String, Object> ai) {
        int valid = ((Number) corpus.get("validCases")).intValue();
        int snapshots = ((Number) corpus.get("snapshotsComplete")).intValue();
        boolean promoted = Boolean.TRUE.equals(ai.get("promotedSetCurrent"));
        return List.of(
                Map.of("id", "candidates", "label", "Candidatos reais", "completed", corpus.get("candidatesFound"), "target", 50, "status", "in_progress"),
                Map.of("id", "snapshots", "label", "Snapshots completos", "completed", snapshots, "target", 50, "status", snapshots >= 50 ? "ready" : "in_progress"),
                Map.of("id", "valid", "label", "Casos validos", "completed", valid, "target", 50, "status", valid >= 50 ? "ready" : "blocked"),
                Map.of("id", "offline", "label", "Benchmark offline", "completed", latest == null ? 0 : latest.getEvaluatedCases(), "target", 50, "status", latest != null && latest.getEvaluatedCases() >= 50 ? "ready" : "pending"),
                Map.of("id", "artifacts", "label", "Artefatos GPT", "completed", ai.get("latestJob") == null ? 0 : 1, "target", 1, "status", ai.get("latestJob") == null ? "pending" : "in_progress"),
                Map.of("id", "promoted", "label", "Conjunto promovido", "completed", promoted ? 1 : 0, "target", 1, "status", promoted ? "ready" : "blocked")
        );
    }

    private int archidektCandidateCount() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("recommendation-benchmark/archidekt-candidates.json")) {
            return input == null ? 0 : objectMapper.readTree(input).path("count").asInt(0);
        } catch (Exception exception) {
            return 0;
        }
    }

    private RecommendationBenchmarkRun requireLatest() {
        RecommendationBenchmarkRun latest = runRepository.latestSuccessful();
        if (latest == null) throw new NotFoundException("No successful benchmark run");
        return latest;
    }

    private JsonNode fixtureRoot() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("benchmark_fixture_missing");
            return objectMapper.readTree(input);
        } catch (Exception exception) {
            throw new IllegalStateException("benchmark_fixture_invalid", exception);
        }
    }

    private List<RecommendationBenchmarkMetricDTO> readMetrics(String json) {
        try {
            return objectMapper.readerForListOf(RecommendationBenchmarkMetricDTO.class).readValue(json);
        } catch (Exception exception) {
            return pendingMetrics();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readList(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Set<String> textSet(JsonNode node) {
        if (!node.isArray()) return Set.of();
        return java.util.stream.StreamSupport.stream(node.spliterator(), false).map(JsonNode::asText).map(this::normalize).collect(Collectors.toSet());
    }

    private boolean hasText(JsonNode node, String field) {
        return node.hasNonNull(field) && !node.path(field).asText().isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double ratio(int value, int total) {
        return total <= 0 ? 0 : value / (double) total;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("benchmark_serialization_failed", exception);
        }
    }

    private long feedbackCount(String status) {
        return auditRepository == null ? 0 : auditRepository.count("feedbackStatus", status);
    }

    private static class Totals {
        int addHits, addCount, cutHits, cutCount, actionable, recommendations, preferencesMet, preferencesDeclared, violations;
        int violations() { return violations; }
    }

    private record CaseMetrics(int addHits, int addCount, int cutHits, int cutCount, int actionable, int recommendations, int preferencesMet, int preferencesDeclared, int violations) {}
}
