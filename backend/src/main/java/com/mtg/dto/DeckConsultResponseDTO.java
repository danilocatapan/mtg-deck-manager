package com.mtg.dto;

import com.mtg.model.DeckVisibility;

import java.util.List;

public record DeckConsultResponseDTO(
        Long id,
        String name,
        String commander,
        String colorIdentity,
        DeckVisibility visibility,
        List<DeckCardDTO> cards,
        List<CommanderDTO> commanders,
        String author
) {
}
