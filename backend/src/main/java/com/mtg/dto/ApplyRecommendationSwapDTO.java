package com.mtg.dto;

public record ApplyRecommendationSwapDTO(
        String add,
        String remove,
        String recommendationId,
        String source,
        String confidence,
        String problem,
        String risk,
        String impactSummary
) {
    public ApplyRecommendationSwapDTO(String add, String remove) {
        this(add, remove, null, null, null, null, null, null);
    }
}
