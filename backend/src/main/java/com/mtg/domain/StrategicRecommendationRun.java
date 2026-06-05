package com.mtg.domain;

import java.util.List;

public record StrategicRecommendationRun(
        String confidence,
        RecommendationCoverage coverage,
        String dataFreshness,
        RecommendationSourceSummary sourceSummary,
        List<String> limitations,
        String benchmarkStatus,
        RecommendationBenchmarkEvidence benchmarkEvidence,
        Long auditId,
        List<StrategicRecommendation> recommendations
) {
    public StrategicRecommendationRun {
        confidence = confidence == null || confidence.isBlank() ? "low_confidence" : confidence;
        dataFreshness = dataFreshness == null || dataFreshness.isBlank() ? "unknown" : dataFreshness;
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
        benchmarkStatus = benchmarkStatus == null || benchmarkStatus.isBlank() ? "not_proven_against_gpt" : benchmarkStatus;
        benchmarkEvidence = benchmarkEvidence == null ? RecommendationBenchmarkEvidence.notCovered() : benchmarkEvidence;
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }

    public StrategicRecommendationRun(
            String confidence,
            RecommendationCoverage coverage,
            String dataFreshness,
            RecommendationSourceSummary sourceSummary,
            List<String> limitations,
            String benchmarkStatus,
            Long auditId,
            List<StrategicRecommendation> recommendations
    ) {
        this(confidence, coverage, dataFreshness, sourceSummary, limitations, benchmarkStatus,
                RecommendationBenchmarkEvidence.notCovered(), auditId, recommendations);
    }
}
