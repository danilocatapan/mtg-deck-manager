package com.mtg.domain;

import java.util.List;

public record StrategicRecommendation(
        String reasoning,
        String add,
        String remove,
        List<String> tags,
        String source,
        String bracket,
        String confidence
) {
    public StrategicRecommendation(String reasoning, String add, String remove) {
        this(reasoning, add, remove, List.of(), "heuristic_fallback", null, "medium");
    }

    public StrategicRecommendation {
        tags = tags == null ? List.of() : List.copyOf(tags);
        source = source == null || source.isBlank() ? "heuristic_fallback" : source;
        confidence = confidence == null || confidence.isBlank() ? "medium" : confidence;
    }
}
