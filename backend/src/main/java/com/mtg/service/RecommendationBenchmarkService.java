package com.mtg.service;

import com.mtg.dto.RecommendationBenchmarkCoverageDTO;
import com.mtg.dto.RecommendationBenchmarkMetricDTO;
import com.mtg.dto.RecommendationBenchmarkNextActionDTO;
import com.mtg.dto.RecommendationBenchmarkSummaryDTO;
import com.mtg.repository.RecommendationAuditRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RecommendationBenchmarkService {
    private static final int TARGET_CASES = 50;

    @Inject
    RecommendationAuditRepository auditRepository;

    public RecommendationBenchmarkSummaryDTO summary() {
        List<RecommendationBenchmarkCoverageDTO> coverage = List.of(
                new RecommendationBenchmarkCoverageDTO("Xenagos, God of Revels", "mid", 2, "curated_reference"),
                new RecommendationBenchmarkCoverageDTO("K'rrik, Son of Yawgmoth", "cedh", 2, "curated_reference"),
                new RecommendationBenchmarkCoverageDTO("Grand Arbiter Augustin IV", "high-power", 2, "curated_reference"),
                new RecommendationBenchmarkCoverageDTO("Kess, Dissident Mage", "cedh", 2, "curated_reference")
        );
        int totalCases = coverage.stream().mapToInt(RecommendationBenchmarkCoverageDTO::getCases).sum();
        Map<String, Long> feedback = Map.of(
                "accepted", feedbackCount("accepted"),
                "rejected", feedbackCount("rejected"),
                "needsReview", feedbackCount("needs_review")
        );
        long reviewedFeedback = feedback.values().stream().mapToLong(Long::longValue).sum();
        return new RecommendationBenchmarkSummaryDTO(
                totalCases >= TARGET_CASES ? "benchmark_ready" : "benchmark_seed",
                "manual_gpt_fixtures_versioned_not_source_of_truth",
                totalCases,
                TARGET_CASES,
                coverage,
                List.of(
                        new RecommendationBenchmarkMetricDTO("commander_legality", "tracked_in_tests", "100%", "required", totalCases),
                        new RecommendationBenchmarkMetricDTO("invalid_commander_cuts", "tracked_in_tests", "0", "required", totalCases),
                        new RecommendationBenchmarkMetricDTO("addPrecisionAt10", "pending_curated_labels", ">= 0.70", "not_ready"),
                        new RecommendationBenchmarkMetricDTO("cutPrecisionAt10", "pending_curated_labels", ">= 0.70", "not_ready"),
                        new RecommendationBenchmarkMetricDTO("preferenceAdherenceRate", "pending_curated_labels", ">= 0.80", "not_ready"),
                        new RecommendationBenchmarkMetricDTO("blind_preference_over_gpt", "pending_human_review", ">= 0.60", "not_ready")
                ),
                List.of(
                        "Benchmark inicial ainda e pequeno; nao sustenta claim global de melhor que GPT.",
                        "Respostas GPT versionadas sao baseline comparativo manual, nao fonte de verdade.",
                        "Comandantes/brackets fora da cobertura devem continuar com benchmarkStatus=not_proven_against_gpt."
                ),
                totalCases,
                0,
                feedback,
                List.of(
                        new RecommendationBenchmarkNextActionDTO(
                                "expand-corpus", "Expandir corpus versionado", totalCases >= TARGET_CASES ? "ready" : "in_progress",
                                "maintainer", "Adicionar casos representativos sem depender de importacao manual de top decks.",
                                totalCases, TARGET_CASES, "documentation"
                        ),
                        new RecommendationBenchmarkNextActionDTO(
                                "human-review", "Realizar avaliacao humana cega", "blocked",
                                "reviewer", "Preencher respostas GPT versionadas e registrar systemWins, gptWins ou tie.",
                                0, totalCases, "human_review"
                        ),
                        new RecommendationBenchmarkNextActionDTO(
                                "collect-feedback", "Coletar feedback de recomendacoes", reviewedFeedback > 0 ? "in_progress" : "pending",
                                "user", "Usuarios avaliam cada rodada como util, nao util ou precisa revisao.",
                                Math.toIntExact(reviewedFeedback), null, "product_feedback"
                        )
                )
        );
    }

    private long feedbackCount(String status) {
        return auditRepository == null ? 0 : auditRepository.count("feedbackStatus", status);
    }
}
