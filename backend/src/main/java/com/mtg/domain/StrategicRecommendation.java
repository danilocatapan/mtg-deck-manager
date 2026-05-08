package com.mtg.domain;

import java.util.List;
import java.util.Map;

public record StrategicRecommendation(
        String id,
        String reasoning,
        String problem,
        String add,
        String remove,
        String risk,
        Map<String, Double> numericImpact,
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
                null,
                reasoning,
                null,
                add,
                remove,
                null,
                Map.of(),
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
        id = id == null || id.isBlank() ? defaultId(add, remove) : id;
        problem = problem == null || problem.isBlank() ? defaultProblem(reasoning) : problem;
        risk = risk == null || risk.isBlank() ? "Risco baixo: troca reversivel, mas valide curva, tema e disponibilidade antes de manter." : risk;
        numericImpact = numericImpact == null ? Map.of() : Map.copyOf(numericImpact);
        tags = tags == null ? List.of() : List.copyOf(tags);
        source = source == null || source.isBlank() ? "heuristic_fallback" : source;
        confidence = confidence == null || confidence.isBlank() ? "medium" : confidence;
        type = type == null || type.isBlank() ? "swap" : type;
        sourceContext = sourceContext == null ? new RecommendationSourceContext(source, 0, List.of()) : sourceContext;
        comparisons = comparisons == null ? List.of() : List.copyOf(comparisons);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    private static String defaultId(String add, String remove) {
        return (safe(add) + "__" + safe(remove)).replaceAll("[^a-z0-9]+", "-");
    }

    private static String defaultProblem(String reasoning) {
        if (reasoning == null || reasoning.isBlank()) {
            return "O deck tem uma oportunidade de troca incremental.";
        }
        String[] parts = reasoning.split("\\.");
        return parts.length == 0 || parts[0].isBlank() ? reasoning : parts[0].trim() + ".";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
