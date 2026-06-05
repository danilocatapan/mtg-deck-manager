package com.mtg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mtg.client.OpenAiResponsesClient;
import com.mtg.dto.RecommendationBenchmarkAiJobDTO;
import com.mtg.dto.RecommendationBenchmarkAiPreviewDTO;
import com.mtg.dto.RecommendationBenchmarkComparisonDTO;
import com.mtg.model.RecommendationBenchmarkAiArtifact;
import com.mtg.model.RecommendationBenchmarkAiJob;
import com.mtg.model.RecommendationBenchmarkAiSet;
import com.mtg.repository.RecommendationBenchmarkAiArtifactRepository;
import com.mtg.repository.RecommendationBenchmarkAiJobRepository;
import com.mtg.repository.RecommendationBenchmarkAiSetRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class RecommendationBenchmarkAiService {
    private static final Logger LOG = Logger.getLogger(RecommendationBenchmarkAiService.class);
    private static final String RESOURCE = "recommendation-benchmark/cases-v1.json";
    private static final String PROMPT_VERSION = "commander-benchmark-ai-v1";
    private static final int JUDGMENTS_PER_BASELINE = 3;
    private static final int BASELINES_PER_CASE = 2;
    private static final int CALLS_PER_CASE = BASELINES_PER_CASE + (BASELINES_PER_CASE * JUDGMENTS_PER_BASELINE);

    @Inject ObjectMapper objectMapper;
    @Inject @RestClient OpenAiResponsesClient client;
    @Inject RecommendationBenchmarkAiJobRepository jobRepository;
    @Inject RecommendationBenchmarkAiArtifactRepository artifactRepository;
    @Inject RecommendationBenchmarkAiSetRepository setRepository;
    @Inject RecommendationBenchmarkScenarioService scenarioService;

    @ConfigProperty(name = "benchmark.openai.api-key") Optional<String> apiKey;
    @ConfigProperty(name = "benchmark.openai.model", defaultValue = "gpt-5.5") String model;
    @ConfigProperty(name = "benchmark.openai.enabled", defaultValue = "false") boolean enabled;

    private final ExecutorService executor = Executors.newFixedThreadPool(2, Thread.ofPlatform().name("benchmark-ai-", 0).factory());

    public RecommendationBenchmarkAiPreviewDTO preview() {
        JsonNode root = fixtureRoot();
        int cases = root.path("cases").size();
        boolean corpusReady = corpusReady(root);
        return new RecommendationBenchmarkAiPreviewDTO(
                configured(),
                model,
                root.path("fixtureVersion").asText("unknown"),
                root.path("algorithmVersion").asText("unknown"),
                PROMPT_VERSION,
                cases,
                cases * BASELINES_PER_CASE,
                cases * BASELINES_PER_CASE * JUDGMENTS_PER_BASELINE,
                cases * CALLS_PER_CASE,
                2,
                !corpusReady ? "corpus_not_ready" : configured() ? "ready_to_generate" : "missing_openai_api_key"
        );
    }

    public RecommendationBenchmarkAiJobDTO start() {
        if (!configured()) throw new IllegalStateException("openai_not_configured");
        RecommendationBenchmarkAiPreviewDTO preview = preview();
        if (!"ready_to_generate".equals(preview.status())) throw new IllegalStateException(preview.status());
        RecommendationBenchmarkAiJob job = QuarkusTransaction.requiringNew().call(() -> {
            if (jobRepository.hasRunning()) throw new IllegalStateException("ai_artifact_job_already_running");
            RecommendationBenchmarkAiJob created = new RecommendationBenchmarkAiJob();
            created.setStatus("running");
            created.setModel(model);
            created.setFixtureVersion(preview.fixtureVersion());
            created.setAlgorithmVersion(preview.algorithmVersion());
            created.setPromptVersion(PROMPT_VERSION);
            created.setTotalCalls(preview.totalCalls());
            created.setCompletedCalls(0);
            created.setFailedCalls(0);
            created.setStartedAt(OffsetDateTime.now());
            jobRepository.persistAndFlush(created);
            return created;
        });
        LOG.infov("event=benchmark.ai_job.started jobId={0} calls={1} model={2}", job.getId(), job.getTotalCalls(), model);
        executor.submit(() -> process(job.getId()));
        return toDto(job, false);
    }

    public RecommendationBenchmarkAiJobDTO job(Long id) {
        RecommendationBenchmarkAiJob job = jobRepository.findById(id);
        if (job == null) throw new NotFoundException("AI artifact job not found");
        RecommendationBenchmarkAiSet promoted = setRepository.latestPromoted();
        return toDto(job, promoted != null && id.equals(promoted.getJobId()));
    }

    public RecommendationBenchmarkComparisonDTO comparison(String caseId) {
        RecommendationBenchmarkAiSet promoted = setRepository.latestPromoted();
        if (promoted == null) throw new NotFoundException("No promoted AI artifact set");
        List<RecommendationBenchmarkAiArtifact> artifacts = artifactRepository.byJob(promoted.getJobId()).stream()
                .filter(item -> caseId.equals(item.getCaseId()))
                .toList();
        if (artifacts.isEmpty()) throw new NotFoundException("Benchmark comparison not found");
        JsonNode fixture = caseById(caseId);
        Map<String, Object> generic = comparisonSummary(artifacts, "generic");
        Map<String, Object> grounded = comparisonSummary(artifacts, "grounded");
        List<String> issues = collectStrings(artifacts, "criticalIssues");
        List<String> improvements = collectStrings(artifacts, "suggestedImprovements");
        return new RecommendationBenchmarkComparisonDTO(
                caseId,
                fixture.path("commander").asText(),
                fixture.path("bracket").asText(),
                "automatic_qualified_without_human_validation",
                generic,
                grounded,
                issues,
                improvements
        );
    }

    public RecommendationBenchmarkAiSet latestPromoted() {
        return setRepository.latestPromoted();
    }

    public boolean hasCurrentQualifiedSet(int expectedCases) {
        RecommendationBenchmarkAiSet promoted = setRepository.latestPromoted();
        RecommendationBenchmarkAiPreviewDTO preview = preview();
        if (promoted == null || promoted.getTotalCases() != expectedCases
                || !preview.fixtureVersion().equals(promoted.getFixtureVersion())
                || !preview.algorithmVersion().equals(promoted.getAlgorithmVersion())
                || !PROMPT_VERSION.equals(promoted.getPromptVersion())
                || !model.equals(promoted.getModel())) {
            return false;
        }
        JsonNode metrics = parseJson(promoted.getMetricsJson());
        return baselineQualified(metrics.path("generic"), expectedCases)
                && baselineQualified(metrics.path("grounded"), expectedCases);
    }

    public Map<String, Object> statusSummary() {
        RecommendationBenchmarkAiPreviewDTO preview = preview();
        RecommendationBenchmarkAiJob latest = jobRepository.latest();
        RecommendationBenchmarkAiSet promoted = setRepository.latestPromoted();
        boolean current = promoted != null
                && preview.fixtureVersion().equals(promoted.getFixtureVersion())
                && preview.algorithmVersion().equals(promoted.getAlgorithmVersion())
                && PROMPT_VERSION.equals(promoted.getPromptVersion())
                && model.equals(promoted.getModel());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", preview.configured());
        result.put("model", model);
        result.put("promptVersion", PROMPT_VERSION);
        result.put("totalCalls", preview.totalCalls());
        result.put("latestJob", latest == null ? null : toDto(latest, promoted != null && latest.getId().equals(promoted.getJobId())));
        result.put("promotedSetId", promoted == null ? null : promoted.getId());
        result.put("promotedSetCurrent", current);
        result.put("metrics", promoted == null ? Map.of() : parseJson(promoted.getMetricsJson()));
        result.put("humanValidation", "pending");
        return result;
    }

    private void process(Long jobId) {
        try {
            JsonNode root = fixtureRoot();
            for (JsonNode fixture : root.path("cases")) {
                generateCase(jobId, fixture, "generic", false);
                generateCase(jobId, fixture, "grounded", true);
            }
            promote(jobId, root);
            LOG.infov("event=benchmark.ai_job.completed jobId={0} calls={1}", jobId, preview().totalCalls());
        } catch (RuntimeException exception) {
            markFailed(jobId, "ai_artifact_generation_failed");
            LOG.errorv(exception, "event=benchmark.ai_job.failed jobId={0} errorCode=ai_artifact_generation_failed", jobId);
        }
    }

    private void generateCase(Long jobId, JsonNode fixture, String baselineKind, boolean grounded) {
        String baselineType = "baseline_" + baselineKind;
        RecommendationBenchmarkAiArtifact baseline = artifactRepository.byJobCaseAndType(jobId, fixture.path("id").asText(), baselineType);
        if (baseline == null) {
            JsonNode output = callStructured(baselinePrompt(fixture, grounded), baselineSchema());
            persistArtifact(jobId, fixture, baselineType, output);
            baseline = artifactRepository.byJobCaseAndType(jobId, fixture.path("id").asText(), baselineType);
        }
        JsonNode systemOutput = scenarioService.execute(fixture).output();
        for (int index = 1; index <= JUDGMENTS_PER_BASELINE; index++) {
            String judgeType = "judge_" + baselineKind + "_" + index;
            if (artifactRepository.byJobCaseAndType(jobId, fixture.path("id").asText(), judgeType) != null) continue;
            boolean systemIsA = Math.floorMod((fixture.path("id").asText() + baselineKind + index).hashCode(), 2) == 0;
            JsonNode baselineOutput = parseJson(baseline.getOutputJson());
            JsonNode veto = deterministicVeto(fixture, systemOutput, baselineOutput, systemIsA);
            if (veto != null) {
                persistArtifact(jobId, fixture, judgeType, veto);
            } else {
                JsonNode anonymousOutput = callStructured(judgePrompt(fixture, systemOutput, baselineOutput, systemIsA), judgeSchema());
                persistArtifact(jobId, fixture, judgeType, mapAnonymousWinner(anonymousOutput, systemIsA));
            }
        }
    }

    private JsonNode callStructured(String prompt, ObjectNode schema) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", model);
        request.put("store", false);
        request.put("input", prompt);
        ObjectNode format = request.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", schema.path("title").asText("benchmark_output"));
        format.put("strict", true);
        format.set("schema", schema.path("schema"));
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                JsonNode response = client.create(apiKey.orElseThrow(), request);
                String text = extractOutputText(response);
                return objectMapper.readTree(text);
            } catch (WebApplicationException exception) {
                int status = exception.getResponse() == null ? 0 : exception.getResponse().getStatus();
                if (status != 429 && status < 500) throw exception;
                last = exception;
            } catch (Exception exception) {
                last = new IllegalStateException("invalid_openai_structured_output", exception);
            }
            try {
                Thread.sleep(attempt * 500L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("ai_artifact_job_interrupted", exception);
            }
        }
        throw last == null ? new IllegalStateException("openai_request_failed") : last;
    }

    private void persistArtifact(Long jobId, JsonNode fixture, String type, JsonNode output) {
        String inputHash = sha256(fixture.toString() + "|" + type + "|" + model + "|" + PROMPT_VERSION);
        QuarkusTransaction.requiringNew().run(() -> {
            RecommendationBenchmarkAiArtifact artifact = new RecommendationBenchmarkAiArtifact();
            artifact.setJobId(jobId);
            artifact.setCaseId(fixture.path("id").asText());
            artifact.setArtifactType(type);
            artifact.setInputHash(inputHash);
            artifact.setModel(model);
            artifact.setPromptVersion(PROMPT_VERSION);
            artifact.setOutputJson(toJson(output));
            artifact.setCreatedAt(OffsetDateTime.now());
            artifactRepository.persist(artifact);
            RecommendationBenchmarkAiJob job = jobRepository.findById(jobId);
            job.setCompletedCalls(job.getCompletedCalls() + 1);
        });
    }

    private void promote(Long jobId, JsonNode root) {
        QuarkusTransaction.requiringNew().run(() -> {
            RecommendationBenchmarkAiJob job = jobRepository.findById(jobId);
            if (job.getCompletedCalls() != job.getTotalCalls()) {
                throw new IllegalStateException("ai_artifact_set_incomplete");
            }
            setRepository.update("status = 'stale' where status = 'promoted'");
            RecommendationBenchmarkAiSet set = new RecommendationBenchmarkAiSet();
            set.setJobId(jobId);
            set.setStatus("promoted");
            set.setModel(model);
            set.setFixtureVersion(root.path("fixtureVersion").asText());
            set.setAlgorithmVersion(root.path("algorithmVersion").asText());
            set.setPromptVersion(PROMPT_VERSION);
            set.setTotalCases(root.path("cases").size());
            set.setPromotedAt(OffsetDateTime.now());
            set.setMetricsJson(toJson(aiMetrics(jobId)));
            setRepository.persist(set);
            job.setStatus("success");
            job.setFinishedAt(OffsetDateTime.now());
        });
    }

    private void markFailed(Long jobId, String code) {
        QuarkusTransaction.requiringNew().run(() -> {
            RecommendationBenchmarkAiJob job = jobRepository.findById(jobId);
            if (job == null) return;
            job.setStatus("failed");
            job.setFailedCalls(Math.max(1, job.getTotalCalls() - job.getCompletedCalls()));
            job.setErrorCode(code);
            job.setFinishedAt(OffsetDateTime.now());
        });
    }

    private Map<String, Object> aiMetrics(Long jobId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generic", aggregateJudges(artifactRepository.byJob(jobId), "generic"));
        result.put("grounded", aggregateJudges(artifactRepository.byJob(jobId), "grounded"));
        result.put("humanValidation", "pending");
        return result;
    }

    private Map<String, Object> aggregateJudges(List<RecommendationBenchmarkAiArtifact> artifacts, String kind) {
        Map<String, List<RecommendationBenchmarkAiArtifact>> byCase = artifacts.stream()
                .filter(item -> item.getArtifactType().startsWith("judge_" + kind + "_"))
                .collect(Collectors.groupingBy(RecommendationBenchmarkAiArtifact::getCaseId));
        int system = 0, gpt = 0, ties = 0, consensus = 0;
        for (List<RecommendationBenchmarkAiArtifact> judges : byCase.values()) {
            long systemVotes = judges.stream().filter(item -> "system".equals(parseJson(item.getOutputJson()).path("winner").asText())).count();
            long gptVotes = judges.stream().filter(item -> "gpt".equals(parseJson(item.getOutputJson()).path("winner").asText())).count();
            long tieVotes = judges.size() - systemVotes - gptVotes;
            if (systemVotes > gptVotes && systemVotes > tieVotes) system++;
            else if (gptVotes > systemVotes && gptVotes > tieVotes) gpt++;
            else ties++;
            if (systemVotes == judges.size() || gptVotes == judges.size() || tieVotes == judges.size()) consensus++;
        }
        int cases = byCase.size();
        return Map.of(
                "cases", cases,
                "systemWins", system,
                "gptWins", gpt,
                "ties", ties,
                "consensusCases", consensus,
                "systemWinRate", cases == 0 ? 0 : system / (double) cases,
                "tieRate", cases == 0 ? 0 : ties / (double) cases
        );
    }

    private Map<String, Object> comparisonSummary(List<RecommendationBenchmarkAiArtifact> artifacts, String kind) {
        return aggregateJudges(artifacts, kind);
    }

    private List<String> collectStrings(List<RecommendationBenchmarkAiArtifact> artifacts, String field) {
        List<String> values = new ArrayList<>();
        for (RecommendationBenchmarkAiArtifact artifact : artifacts) {
            if (!artifact.getArtifactType().startsWith("judge_")) continue;
            parseJson(artifact.getOutputJson()).path(field).forEach(item -> {
                String text = item.asText();
                if (!text.isBlank() && !values.contains(text)) values.add(text);
            });
        }
        return values.stream().limit(12).toList();
    }

    private String baselinePrompt(JsonNode fixture, boolean grounded) {
        StringBuilder prompt = new StringBuilder("""
                Analyze this Commander deck and return concrete add/cut swaps with reasoning and risk.
                Respect Commander legality, bracket, budget, collection and explicit preferences.
                Return only the requested structured JSON.
                CASE:
                """).append(fixture.path("id").asText()).append("\nCOMMANDER: ").append(fixture.path("commander").asText())
                .append("\nBRACKET: ").append(fixture.path("bracket").asText())
                .append("\nSTRATEGY: ").append(fixture.path("strategy").asText())
                .append("\nDECK: ").append(fixture.path("deck"));
        if (grounded) prompt.append("\nCATALOG: ").append(fixture.path("catalog")).append("\nMETA: ").append(fixture.path("meta"));
        return prompt.toString();
    }

    private String judgePrompt(JsonNode fixture, JsonNode system, JsonNode baseline, boolean systemIsA) {
        JsonNode optionA = systemIsA ? system : baseline;
        JsonNode optionB = systemIsA ? baseline : system;
        return """
                Judge two anonymous Commander deck analyses. Prefer legality, strategic fit, cut quality,
                preference adherence, deck coherence, actionability, explanation and risk awareness.
                Return winner as A, B or tie.
                Never infer identity from writing style.
                """ + "\nCASE: " + fixture + "\nOPTION_A: " + optionA + "\nOPTION_B: " + optionB;
    }

    private ObjectNode baselineSchema() {
        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.put("title", "commander_baseline");
        ObjectNode schema = wrapper.putObject("schema");
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode recommendations = properties.putObject("recommendations");
        recommendations.put("type", "array");
        ObjectNode item = recommendations.putObject("items");
        item.put("type", "object");
        ObjectNode fields = item.putObject("properties");
        for (String name : List.of("add", "remove", "reasoning", "risk")) fields.putObject(name).put("type", "string");
        item.set("required", objectMapper.valueToTree(List.of("add", "remove", "reasoning", "risk")));
        item.put("additionalProperties", false);
        schema.set("required", objectMapper.valueToTree(List.of("recommendations")));
        schema.put("additionalProperties", false);
        return wrapper;
    }

    private ObjectNode judgeSchema() {
        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.put("title", "commander_judgment");
        ObjectNode schema = wrapper.putObject("schema");
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("winner").put("type", "string").set("enum", objectMapper.valueToTree(List.of("A", "B", "tie")));
        properties.putObject("confidence").put("type", "number");
        properties.putObject("criticalIssues").put("type", "array").putObject("items").put("type", "string");
        properties.putObject("suggestedImprovements").put("type", "array").putObject("items").put("type", "string");
        ObjectNode scores = properties.putObject("scores");
        scores.put("type", "object");
        ObjectNode scoreProperties = scores.putObject("properties");
        for (String category : List.of(
                "strategicFit",
                "cutQuality",
                "preferenceAdherence",
                "deckCoherence",
                "actionability",
                "explanationAndRisk"
        )) {
            scoreProperties.putObject(category).put("type", "number");
        }
        scores.set("required", objectMapper.valueToTree(List.of(
                "strategicFit",
                "cutQuality",
                "preferenceAdherence",
                "deckCoherence",
                "actionability",
                "explanationAndRisk"
        )));
        scores.put("additionalProperties", false);
        schema.set("required", objectMapper.valueToTree(List.of("winner", "confidence", "criticalIssues", "suggestedImprovements", "scores")));
        schema.put("additionalProperties", false);
        return wrapper;
    }

    private JsonNode mapAnonymousWinner(JsonNode anonymousOutput, boolean systemIsA) {
        ObjectNode mapped = anonymousOutput.deepCopy();
        String anonymousWinner = anonymousOutput.path("winner").asText("tie");
        String winner = switch (anonymousWinner) {
            case "A" -> systemIsA ? "system" : "gpt";
            case "B" -> systemIsA ? "gpt" : "system";
            default -> "tie";
        };
        mapped.put("winner", winner);
        mapped.put("anonymousWinner", anonymousWinner);
        mapped.put("systemOption", systemIsA ? "A" : "B");
        return mapped;
    }

    private JsonNode deterministicVeto(JsonNode fixture, JsonNode systemOutput, JsonNode baselineOutput, boolean systemIsA) {
        RecommendationBenchmarkScenarioService.Validation system = scenarioService.validate(fixture, systemOutput);
        RecommendationBenchmarkScenarioService.Validation gpt = scenarioService.validate(fixture, baselineOutput.path("recommendations"));
        if (system.passed() && gpt.passed()) return null;
        String winner = system.passed() ? "system" : gpt.passed() ? "gpt" : "tie";
        ObjectNode result = objectMapper.createObjectNode();
        result.put("winner", winner);
        result.put("anonymousWinner", "system".equals(winner) ? (systemIsA ? "A" : "B") : "gpt".equals(winner) ? (systemIsA ? "B" : "A") : "tie");
        result.put("systemOption", systemIsA ? "A" : "B");
        result.put("confidence", 1.0);
        ArrayNode issues = result.putArray("criticalIssues");
        system.violations().forEach(value -> issues.add("system:" + value));
        gpt.violations().forEach(value -> issues.add("gpt:" + value));
        result.putArray("suggestedImprovements").add("Corrigir violacoes objetivas antes de comparar qualidade estrategica.");
        result.putObject("scores");
        result.put("deterministicVeto", true);
        return result;
    }

    private String extractOutputText(JsonNode response) {
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText())) return content.path("text").asText();
            }
        }
        throw new IllegalStateException("openai_output_text_missing");
    }

    private JsonNode caseById(String caseId) {
        for (JsonNode fixture : fixtureRoot().path("cases")) {
            if (caseId.equals(fixture.path("id").asText())) return fixture;
        }
        throw new NotFoundException("Benchmark case not found");
    }

    private JsonNode fixtureRoot() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("benchmark_fixture_missing");
            return objectMapper.readTree(input);
        } catch (Exception exception) {
            throw new IllegalStateException("benchmark_fixture_invalid", exception);
        }
    }

    private JsonNode parseJson(String value) {
        try { return objectMapper.readTree(value); }
        catch (Exception exception) { return objectMapper.createObjectNode(); }
    }

    private boolean baselineQualified(JsonNode baseline, int expectedCases) {
        return baseline.path("cases").asInt() == expectedCases
                && baseline.path("systemWinRate").asDouble() >= 0.60
                && baseline.path("tieRate").asDouble(1.0) <= 0.20;
    }

    private boolean configured() {
        return enabled && apiKey.isPresent() && !apiKey.get().isBlank();
    }

    private boolean corpusReady(JsonNode root) {
        if (root.path("cases").size() != 50) return false;
        java.util.Set<String> commanders = new java.util.HashSet<>();
        for (JsonNode fixture : root.path("cases")) {
            if (!scenarioService.validateFixture(fixture).isEmpty()) return false;
            if (!commanders.add(fixture.path("commander").asText().trim().toLowerCase(java.util.Locale.ROOT))) return false;
        }
        return true;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("benchmark_hash_failed", exception);
        }
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("benchmark_serialization_failed", exception); }
    }

    private RecommendationBenchmarkAiJobDTO toDto(RecommendationBenchmarkAiJob job, boolean promoted) {
        return new RecommendationBenchmarkAiJobDTO(job.getId(), job.getStatus(), job.getModel(), job.getTotalCalls(), job.getCompletedCalls(), job.getFailedCalls(), job.getStartedAt(), job.getFinishedAt(), job.getErrorCode(), promoted);
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }
}
