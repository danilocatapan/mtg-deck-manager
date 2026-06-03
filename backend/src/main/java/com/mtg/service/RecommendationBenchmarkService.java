package com.mtg.service;

import com.mtg.dto.RecommendationBenchmarkCoverageDTO;
import com.mtg.dto.RecommendationBenchmarkMetricDTO;
import com.mtg.dto.RecommendationBenchmarkSummaryDTO;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class RecommendationBenchmarkService {
    private static final int TARGET_CASES = 50;

    public RecommendationBenchmarkSummaryDTO summary() {
        List<RecommendationBenchmarkCoverageDTO> coverage = List.of(
                new RecommendationBenchmarkCoverageDTO("Xenagos, God of Revels", "mid", 2, "curated_reference"),
                new RecommendationBenchmarkCoverageDTO("K'rrik, Son of Yawgmoth", "cedh", 2, "curated_reference"),
                new RecommendationBenchmarkCoverageDTO("Grand Arbiter Augustin IV", "high-power", 2, "curated_reference"),
                new RecommendationBenchmarkCoverageDTO("Kess, Dissident Mage", "cedh", 2, "curated_reference")
        );
        int totalCases = coverage.stream().mapToInt(RecommendationBenchmarkCoverageDTO::getCases).sum();
        return new RecommendationBenchmarkSummaryDTO(
                totalCases >= TARGET_CASES ? "benchmark_ready" : "benchmark_seed",
                "manual_gpt_fixtures_versioned_not_source_of_truth",
                totalCases,
                TARGET_CASES,
                coverage,
                List.of(
                        new RecommendationBenchmarkMetricDTO("commander_legality", "tracked_in_tests", "100%", "required"),
                        new RecommendationBenchmarkMetricDTO("invalid_commander_cuts", "tracked_in_tests", "0", "required"),
                        new RecommendationBenchmarkMetricDTO("addPrecisionAt10", "pending_curated_labels", ">= 0.70", "not_ready"),
                        new RecommendationBenchmarkMetricDTO("cutPrecisionAt10", "pending_curated_labels", ">= 0.70", "not_ready"),
                        new RecommendationBenchmarkMetricDTO("preferenceAdherenceRate", "pending_curated_labels", ">= 0.80", "not_ready"),
                        new RecommendationBenchmarkMetricDTO("blind_preference_over_gpt", "pending_human_review", ">= 0.60", "not_ready")
                ),
                List.of(
                        "Benchmark inicial ainda e pequeno; nao sustenta claim global de melhor que GPT.",
                        "Respostas GPT versionadas sao baseline comparativo manual, nao fonte de verdade.",
                        "Comandantes/brackets fora da cobertura devem continuar com benchmarkStatus=not_proven_against_gpt."
                )
        );
    }
}
