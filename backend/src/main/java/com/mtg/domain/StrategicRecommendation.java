package com.mtg.domain;

import java.util.List;

public record StrategicRecommendation(
        String reasoning,
        String add,
        String remove,
        List<String> tags,
        String source,
        String bracket,
        String confidence,
        String type,
        RecommendationSourceContext sourceContext,
        RecommendationImpact impact,
        RecommendationCardInsight addInsight,
        RecommendationCardInsight removeInsight,
        List<MetaComparison> comparisons,
        List<RecommendationWarning> warnings,
        String recommendationMode,
        Double budget
) {
    public StrategicRecommendation(String reasoning, String add, String remove) {
        this(reasoning, add, remove, List.of(), "heuristic_fallback", null, "medium");
    }

    public StrategicRecommendation(
            String reasoning,
            String add,
            String remove,
            List<String> tags,
            String source,
            String bracket,
            String confidence
    ) {
        this(
                reasoning,
                add,
                remove,
                tags,
                source,
                bracket,
                confidence,
                "swap",
                new RecommendationSourceContext(source, 0, List.of()),
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                null
        );
    }

    public StrategicRecommendation {
        tags = tags == null ? List.of() : List.copyOf(tags);
        source = source == null || source.isBlank() ? "heuristic_fallback" : source;
        confidence = confidence == null || confidence.isBlank() ? "medium" : confidence;
        type = type == null || type.isBlank() ? "swap" : type;
        sourceContext = sourceContext == null ? new RecommendationSourceContext(source, 0, List.of()) : sourceContext;
        comparisons = comparisons == null ? List.of() : List.copyOf(comparisons);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
