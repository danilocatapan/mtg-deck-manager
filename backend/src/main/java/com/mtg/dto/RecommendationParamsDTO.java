package com.mtg.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RecommendationParams")
public record RecommendationParamsDTO(
        Double budget,
        String bracket,
        String strategy,
        String meta,
        String sourceMode,
        Integer maxRecommendations,
        Boolean ownedOnly,
        Boolean avoidSalt,
        Boolean avoidTutors,
        Boolean improveMana,
        Boolean lowerCurve,
        Boolean moreInteraction,
        Boolean preserveTheme
) {
    public RecommendationParamsDTO(Double budget, String bracket, String strategy, String meta) {
        this(budget, bracket, strategy, meta, null, null);
    }

    public RecommendationParamsDTO(Double budget, String bracket, String strategy, String meta, String sourceMode, Integer maxRecommendations) {
        this(budget, bracket, strategy, meta, sourceMode, maxRecommendations, null, null, null, null, null, null, null);
    }
}
