package com.mtg.domain;

import java.util.List;

public record RecommendationSourceContext(
        String kind,
        int sampleSize,
        List<String> sources
) {
    public RecommendationSourceContext {
        kind = kind == null || kind.isBlank() ? "heuristic_fallback" : kind;
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
