package com.mtg.dto;

public record MetaTopDeckCardRequestDTO(
        String name,
        int quantity,
        String section
) {
}
