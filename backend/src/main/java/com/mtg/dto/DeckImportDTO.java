package com.mtg.dto;

import java.util.List;

public record DeckImportDTO(String name, String commander, String content, List<CommanderDTO> commanders) {
    public DeckImportDTO(String name, String commander, String content) {
        this(name, commander, content, null);
    }
}
