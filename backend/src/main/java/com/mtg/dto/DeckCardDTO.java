package com.mtg.dto;

public record DeckCardDTO(
        String name,
        int quantity,
        String scryfallId,
        String setCode,
        String setName,
        String collectorNumber,
        String finish,
        String imageUrl
) {
    public DeckCardDTO(String name, int quantity) {
        this(name, quantity, null, null, null, null, null, null);
    }
}
