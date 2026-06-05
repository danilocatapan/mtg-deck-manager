package com.mtg.domain;

import java.time.OffsetDateTime;

public record RecommendationBenchmarkEvidence(
        String status,
        Double genericWinRate,
        Double groundedWinRate,
        String fixtureVersion,
        OffsetDateTime evaluatedAt,
        String humanValidation
) {
    public RecommendationBenchmarkEvidence {
        status = status == null || status.isBlank() ? "not_covered" : status;
        humanValidation = humanValidation == null || humanValidation.isBlank() ? "pending" : humanValidation;
    }

    public static RecommendationBenchmarkEvidence notCovered() {
        return new RecommendationBenchmarkEvidence("not_covered", null, null, null, null, "pending");
    }
}
