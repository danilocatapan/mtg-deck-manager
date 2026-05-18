package com.mtg.dto;

import com.mtg.model.DeckVisibility;

public record PublicDeckSummaryDTO(
        Long id,
        String name,
        String commander,
        String colorIdentity,
        DeckVisibility visibility,
        String author,
        int cardCount
) {
}
