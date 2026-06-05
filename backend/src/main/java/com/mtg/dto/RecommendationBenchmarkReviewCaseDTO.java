package com.mtg.dto;

import java.util.List;
import java.util.Map;

public record RecommendationBenchmarkReviewCaseDTO(
        Long runId,
        String caseId,
        String commander,
        String bracket,
        List<Map<String, Object>> optionA,
        List<Map<String, Object>> optionB,
        int reviewsCompleted,
        int reviewsRequired
) {
}
