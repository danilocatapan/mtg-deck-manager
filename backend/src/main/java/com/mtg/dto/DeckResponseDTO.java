package com.mtg.dto;

import com.mtg.model.DeckVisibility;

import java.util.List;

public record DeckResponseDTO(
        Long id,
        String name,
        String commander,
        List<DeckCardDTO> cards,
        String colorIdentity,
        List<CommanderDTO> commanders,
        List<DeckHistoryEntryDTO> history,
        DeckVisibility visibility
) {
    public DeckResponseDTO(Long id, String name, String commander, List<DeckCardDTO> cards) {
        this(id, name, commander, cards, null, null);
    }

    public DeckResponseDTO(Long id, String name, String commander, List<DeckCardDTO> cards, String colorIdentity, List<CommanderDTO> commanders) {
        this(id, name, commander, cards, colorIdentity, commanders, List.of(), DeckVisibility.PRIVATE);
    }

    public DeckResponseDTO(Long id, String name, String commander, List<DeckCardDTO> cards, String colorIdentity, List<CommanderDTO> commanders, List<DeckHistoryEntryDTO> history) {
        this(id, name, commander, cards, colorIdentity, commanders, history, DeckVisibility.PRIVATE);
    }
}
