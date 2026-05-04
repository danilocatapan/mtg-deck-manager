package com.mtg.dto;

public record ScryfallCardResponseDTO(
        String name,
        String mana_cost,
        double cmc,
        String type_line
) {
}
