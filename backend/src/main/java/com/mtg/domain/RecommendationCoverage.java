package com.mtg.domain;

import java.util.List;

public record RecommendationCoverage(
        boolean commanderKnown,
        boolean usefulMeta,
        int sampleSize,
        List<String> sourcesUsed,
        int requestedCards,
        int resolvedCards,
        double cardResolutionRate,
        int mainDeckCards,
        String bracket,
        boolean fallbackUsed
) {
    public RecommendationCoverage {
        sourcesUsed = sourcesUsed == null ? List.of() : List.copyOf(sourcesUsed);
        bracket = bracket == null || bracket.isBlank() ? "casual" : bracket;
        cardResolutionRate = Math.max(0.0, Math.min(1.0, cardResolutionRate));
    }
}
