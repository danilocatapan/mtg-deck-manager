package com.mtg.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RecommendationParams")
public record RecommendationParamsDTO(
        Double budget,
        String bracket,
        String strategy,
        String meta
) {
}
