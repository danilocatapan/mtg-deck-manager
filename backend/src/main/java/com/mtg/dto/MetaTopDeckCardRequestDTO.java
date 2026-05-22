package com.mtg.dto;

public record MetaTopDeckCardRequestDTO(
        String name,
        int quantity,
        String section,
        String scryfallId,
        String setCode,
        String setName,
        String collectorNumber,
        String finish,
        String imageUrl
) {
    public MetaTopDeckCardRequestDTO(String name, int quantity, String section) {
        this(name, quantity, section, null, null, null, null, null, null);
    }
}
