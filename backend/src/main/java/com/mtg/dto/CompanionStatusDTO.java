package com.mtg.dto;

public record CompanionStatusDTO(
        boolean present,
        String name,
        boolean legal,
        String reason
) {
}
