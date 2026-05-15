package com.mtg.dto;

import java.time.OffsetDateTime;

public record RecommendationAuditExportDTO(
        Long id,
        Long deckId,
        String commander,
        String colorIdentity,
        String bracket,
        String archetype,
        String algorithmVersion,
        OffsetDateTime createdAt,
        String gapsJson,
        String issuesJson,
        String weakCardsJson,
        String paramsJson,
        String recommendationsJson,
        String blockedPairsJson,
        String protectedCutsJson,
        String feedbackStatus,
        String feedbackReason,
        String feedbackNotes,
        OffsetDateTime feedbackAt
) {
}
