package com.mtg.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TopDeckStandingDTO(
        String name,
        String decklist,
        Integer wins,
        Integer draws,
        Integer losses,
        Double winRate
) {
}
