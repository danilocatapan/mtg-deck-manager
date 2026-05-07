package com.mtg.dto;

import java.util.List;

public record DeckRequestDTO(
        String name,
        String commander,
        List<DeckCardDTO> cards,
        List<CommanderDTO> commanders
) {
    public DeckRequestDTO(String name, String commander, List<DeckCardDTO> cards) {
        this(name, commander, cards, null);
    }
}
