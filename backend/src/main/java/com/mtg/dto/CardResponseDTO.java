package com.mtg.dto;

public record CardResponseDTO(
        String name,
        double cmc,
        String manaCost,
        String type
) {
}
