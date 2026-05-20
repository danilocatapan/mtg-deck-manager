package com.mtg.dto;

import java.util.List;

public record ExternalDeckImportResponseDTO(
        String source,
        String format,
        String importFormat,
        String decklistFormat,
        int importedDecks,
        int ignoredDecks,
        int importedCards,
        List<String> warnings
) {
}
