package com.mtg.dto;

import java.util.List;

public record MetaTopDeckImportDeckDTO(
        Integer rank,
        String name,
        String commander,
        String deckUrl,
        String decklist,
        String archetype,
        String bracket,
        List<String> colorIdentity,
        Integer wins,
        Integer losses,
        Double popularityScore,
        List<MetaTopDeckCardRequestDTO> cards
) {
}
