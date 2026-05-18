package com.mtg.dto;

import com.mtg.model.DeckVisibility;

import java.util.List;

public record DeckImportDTO(String name, String commander, String content, List<CommanderDTO> commanders, DeckVisibility visibility) {
    public DeckImportDTO(String name, String commander, String content) {
        this(name, commander, content, null, null);
    }

    public DeckImportDTO(String name, String commander, String content, List<CommanderDTO> commanders) {
        this(name, commander, content, commanders, null);
    }
}
