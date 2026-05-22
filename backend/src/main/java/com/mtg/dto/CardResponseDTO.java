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
        String imageUrl,
        Double estimatedPrice,
        String scryfallId,
        String setCode,
        String setName,
        String collectorNumber,
        java.util.List<String> finishes
) {
    public CardResponseDTO(
            String name,
            String manaCost,
            String typeLine,
            String oracleText,
            Double cmc,
            java.util.List<String> colorIdentity,
            java.util.List<String> keywords,
            String imageUrl,
            Double estimatedPrice
    ) {
        this(name, manaCost, typeLine, oracleText, cmc, colorIdentity, keywords, imageUrl, estimatedPrice, null, null, null, null, java.util.List.of());
    }

    public CardResponseDTO(
            String name,
            String manaCost,
            String typeLine,
            String oracleText,
            Double cmc,
            java.util.List<String> colorIdentity,
            java.util.List<String> keywords,
            String imageUrl
    ) {
        this(name, manaCost, typeLine, oracleText, cmc, colorIdentity, keywords, imageUrl, null);
    }

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
