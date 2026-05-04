package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "ScryfallResponse")
public record ScryfallResponseDTO(
        List<ScryfallCardDTO> data
) {
}

