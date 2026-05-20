package com.mtg.dto;

import java.util.List;

public record ExternalDeckImportRequestDTO(
        String source,
        String sourceUrl,
        String format,
        String importFormat,
        String decklistFormat,
        List<ExternalDeckImportDeckDTO> decks
) {
}
