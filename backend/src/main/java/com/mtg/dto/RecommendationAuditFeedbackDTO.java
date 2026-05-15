package com.mtg.dto;

public record RecommendationAuditFeedbackDTO(
        String status,
        String reason,
        String notes
) {
}
