package com.mtg.dto;

public record MetaTopDeckCardResponseDTO(
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
    public MetaTopDeckCardResponseDTO(String name, int quantity, String section, String scryfallId) {
        this(name, quantity, section, scryfallId, null, null, null, null, null);
    }
}
