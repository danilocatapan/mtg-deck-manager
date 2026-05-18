package com.mtg.dto;

import com.mtg.model.DeckVisibility;

import java.util.List;

public record DeckRequestDTO(
        String name,
        String commander,
        List<DeckCardDTO> cards,
        List<CommanderDTO> commanders,
        DeckVisibility visibility
) {
    public DeckRequestDTO(String name, String commander, List<DeckCardDTO> cards) {
        this(name, commander, cards, null, null);
    }

    public DeckRequestDTO(String name, String commander, List<DeckCardDTO> cards, List<CommanderDTO> commanders) {
        this(name, commander, cards, commanders, null);
    }
}
