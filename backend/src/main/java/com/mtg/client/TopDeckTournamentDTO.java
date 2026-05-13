package com.mtg.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TopDeckTournamentDTO(
        @JsonAlias({"TID", "id"})
        String id,
        String tournamentName,
        String format,
        @JsonAlias({"participantCount", "participants", "players"})
        Integer participantCount,
        Long startDate,
        String topdeckUrl,
        List<TopDeckStandingDTO> standings
) {
}
