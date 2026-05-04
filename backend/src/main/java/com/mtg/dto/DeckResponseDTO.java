package com.mtg.dto;

import java.util.List;

public record DeckResponseDTO(
        Long id,
        String name,
        String commander,
        List<DeckCardDTO> cards
) {
}
