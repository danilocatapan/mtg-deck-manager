package com.mtg.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CardResponse")
public record CardResponseDTO(
        String name,
        String manaCost,
        String typeLine,
        String oracleText,
        Double cmc
) {
}
