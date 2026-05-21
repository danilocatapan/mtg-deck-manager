package com.mtg.dto;

public record MetaTopDeckCardResponseDTO(
        String name,
        int quantity,
        String section,
        String scryfallId
) {
}
