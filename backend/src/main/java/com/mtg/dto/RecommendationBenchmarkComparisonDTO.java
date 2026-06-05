package com.mtg.dto;

import java.util.List;
import java.util.Map;

public record RecommendationBenchmarkComparisonDTO(
        String caseId,
        String commander,
        String bracket,
        String status,
        Map<String, Object> generic,
        Map<String, Object> grounded,
        List<String> criticalIssues,
        List<String> suggestedImprovements
) {}
