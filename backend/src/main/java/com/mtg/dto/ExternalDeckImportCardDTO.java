package com.mtg.dto;

public record ExternalDeckImportCardDTO(
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
    public ExternalDeckImportCardDTO(String name, int quantity, String section) {
        this(name, quantity, section, null, null, null, null, null, null);
    }
}
