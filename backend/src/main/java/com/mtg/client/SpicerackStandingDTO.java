package com.mtg.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpicerackStandingDTO(
        String name,
        String decklist,
        Integer winsSwiss,
        Integer lossesSwiss,
        Integer draws,
        Integer winsBracket,
        Integer lossesBracket,
        @JsonProperty("decklist_text")
        String decklistText
) {
}
