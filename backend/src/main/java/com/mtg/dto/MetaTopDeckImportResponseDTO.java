package com.mtg.dto;

import java.time.LocalDate;
import java.util.List;

public record MetaTopDeckImportResponseDTO(
        Long batchId,
        String source,
        String rankingPeriod,
        LocalDate rankingDate,
        String format,
        String status,
        int receivedDecks,
        int importedDecks,
        int createdDecks,
        int updatedDecks,
        int ignoredDecks,
        int importedCards,
        int warningsCount,
        String profileRefreshStatus,
        List<String> affectedProfiles,
        List<String> warnings
) {
}
