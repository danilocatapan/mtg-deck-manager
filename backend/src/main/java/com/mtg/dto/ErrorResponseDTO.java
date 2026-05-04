package com.mtg.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ErrorResponse")
public record ErrorResponseDTO(
        String message
) {
}

