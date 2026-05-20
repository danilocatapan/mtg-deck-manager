package com.mtg.dto;

import java.util.List;

public record ExternalDeckImportDeckDTO(
        Integer rank,
        String name,
        String commander,
        String deckUrl,
        String decklist,
        List<ExternalDeckImportCardDTO> cards
) {
}
