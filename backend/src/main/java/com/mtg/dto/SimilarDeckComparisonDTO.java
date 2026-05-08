package com.mtg.dto;

import java.util.List;

public record SimilarDeckComparisonDTO(
        String commander,
        String bracket,
        int sampleSize,
        List<String> sources,
        List<ComparisonMetricDTO> metrics
) {
    public SimilarDeckComparisonDTO {
        sources = sources == null ? List.of() : List.copyOf(sources);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
    }
}
