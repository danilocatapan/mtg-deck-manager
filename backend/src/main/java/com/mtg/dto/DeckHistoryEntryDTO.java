package com.mtg.dto;

public record DeckHistoryEntryDTO(
        String id,
        String add,
        String remove,
        String source,
        String confidence,
        String problem,
        String risk,
        String impactSummary,
        String appliedAt,
        boolean undone
) {
}
