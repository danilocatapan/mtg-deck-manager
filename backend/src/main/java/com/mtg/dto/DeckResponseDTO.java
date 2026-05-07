package com.mtg.dto;

import java.util.List;

public record DeckResponseDTO(
        Long id,
        String name,
        String commander,
        List<DeckCardDTO> cards,
        String colorIdentity,
        List<CommanderDTO> commanders
) {
    public DeckResponseDTO(Long id, String name, String commander, List<DeckCardDTO> cards) {
        this(id, name, commander, cards, null, null);
    }
}
