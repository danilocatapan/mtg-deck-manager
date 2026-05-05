package com.mtg.domain;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RecommendationItem")
public record RecommendationItem(
        String name,
        String role,
        String reason,
        double score,
        double metaScore,
        double synergyScore,
        double efficiencyScore,
        double estimatedPrice,
        int quantity
) {
    public RecommendationItem(
            String name,
            String role,
            String reason,
            double score,
            double metaScore,
            double synergyScore,
            double efficiencyScore,
            double estimatedPrice
    ) {
        this(name, role, reason, score, metaScore, synergyScore, efficiencyScore, estimatedPrice, 1);
    }
}
