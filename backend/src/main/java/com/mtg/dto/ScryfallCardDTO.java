package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "ScryfallCard")
public record ScryfallCardDTO(
        String name,
        @JsonProperty("mana_cost") String manaCost,
        @JsonProperty("cmc") Double cmc,
        @JsonProperty("type_line") String typeLine,
        @JsonProperty("oracle_text") String oracleText
) {
}

