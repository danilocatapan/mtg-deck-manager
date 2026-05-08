package com.mtg.dto;

import java.util.List;

public record DeckPackageDTO(
        String id,
        String name,
        String description,
        String zone,
        List<String> tags,
        List<DeckCardDTO> cards
) {
    public DeckPackageDTO {
        tags = tags == null ? List.of() : List.copyOf(tags);
        cards = cards == null ? List.of() : List.copyOf(cards);
    }
}
