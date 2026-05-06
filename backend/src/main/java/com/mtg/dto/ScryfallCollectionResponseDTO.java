package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScryfallCollectionResponseDTO(
        List<ScryfallCardDTO> data,
        @JsonProperty("not_found") List<ScryfallCollectionRequestDTO.CardIdentifier> notFound
) {
}
