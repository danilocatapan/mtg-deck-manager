package com.mtg.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record UserDataExportDTO(
        OffsetDateTime generatedAt,
        AuthenticatedUserDTO user,
        List<String> collectedData,
        List<DeckResponseDTO> decks,
        List<RecommendationAuditExportDTO> recommendationAudits
) {
}
