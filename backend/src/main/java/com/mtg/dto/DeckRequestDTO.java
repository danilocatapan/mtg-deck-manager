package com.mtg.dto;

import java.util.List;

public record DeckRequestDTO(
        String name,
        String commander,
        List<DeckCardDTO> cards
) {
}
