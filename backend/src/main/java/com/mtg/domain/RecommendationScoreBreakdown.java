package com.mtg.domain;

import java.util.List;

public record RecommendationScoreBreakdown(
        double totalScore,
        double metaScore,
        double synergyScore,
        double efficiencyScore,
        double gapScore,
        double multiplayerScore,
        double curveScore,
        double bracketFitScore,
        List<String> reasons
) {
    public RecommendationScoreBreakdown {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
