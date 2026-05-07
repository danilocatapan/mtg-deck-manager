package com.mtg.domain;

public record RecommendationCardInsight(
        String name,
        String role,
        double inclusionRate,
        int sampleSize,
        double synergyEstimate,
        String source,
        Double estimatedPrice,
        String priceDisclaimer
) {
}
