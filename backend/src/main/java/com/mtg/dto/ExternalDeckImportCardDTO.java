package com.mtg.dto;

public record ExternalDeckImportCardDTO(
        String name,
        int quantity,
        String section
) {
}
