package com.mtg.dto;

import java.util.List;

public record DeckResponseDTO(
        Long id,
        String name,
        String commander,
        List<DeckCardDTO> cards,
        String colorIdentity,
        List<CommanderDTO> commanders,
        List<DeckHistoryEntryDTO> history
) {
    public DeckResponseDTO(Long id, String name, String commander, List<DeckCardDTO> cards) {
        this(id, name, commander, cards, null, null);
    }

    public DeckResponseDTO(Long id, String name, String commander, List<DeckCardDTO> cards, String colorIdentity, List<CommanderDTO> commanders) {
        this(id, name, commander, cards, colorIdentity, commanders, List.of());
    }
}
