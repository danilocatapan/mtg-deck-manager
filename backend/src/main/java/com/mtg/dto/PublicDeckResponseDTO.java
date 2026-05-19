package com.mtg.dto;

import com.mtg.model.DeckVisibility;

import java.util.List;

public record PublicDeckResponseDTO(
        Long id,
        String name,
        String commander,
        List<CommanderDTO> commanders,
        String colorIdentity,
        List<DeckCardDTO> cards,
        int mainDeckSize,
        DeckVisibility visibility,
        String author,
        boolean ownedByCurrentUser
) {
}
