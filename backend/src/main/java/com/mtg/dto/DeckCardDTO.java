package com.mtg.dto;

public record DeckCardDTO(
        String name,
        int quantity,
        String zone
) {
    public DeckCardDTO(String name, int quantity) {
        this(name, quantity, "main");
    }
}
