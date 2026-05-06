package com.mtg.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CardResponse")
public record CardResponseDTO(
        String name,
        String manaCost,
        String typeLine,
        String oracleText,
        Double cmc,
        java.util.List<String> colorIdentity,
        java.util.List<String> keywords,
        String imageUrl
) {
    public CardResponseDTO(
            String name,
            String manaCost,
            String typeLine,
            String oracleText,
            Double cmc,
            java.util.List<String> colorIdentity,
            java.util.List<String> keywords
    ) {
        this(name, manaCost, typeLine, oracleText, cmc, colorIdentity, keywords, null);
    }
}
