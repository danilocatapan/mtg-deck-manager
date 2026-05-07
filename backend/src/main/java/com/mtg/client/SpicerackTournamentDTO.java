package com.mtg.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SpicerackTournamentDTO(
        @JsonProperty("TID")
        String id,
        String tournamentName,
        String format,
        String bracketUrl,
        Integer players,
        Long startDate,
        List<SpicerackStandingDTO> standings
) {
}
