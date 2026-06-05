package com.mtg.dto;

public record RecommendationBenchmarkNextActionDTO(
        String id,
        String title,
        String status,
        String actor,
        String description,
        Integer completed,
        Integer target,
        String actionType
) {
}
