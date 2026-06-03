package com.mtg.domain;

import java.util.List;

public record RecommendationSourceSummary(
        String mode,
        int sampleSize,
        List<String> sources,
        String attribution,
        boolean metaDriven,
        boolean fallbackUsed
) {
    public RecommendationSourceSummary {
        mode = mode == null || mode.isBlank() ? "auto" : mode;
        sources = sources == null ? List.of() : List.copyOf(sources);
        attribution = attribution == null ? "" : attribution;
    }
}
