package com.mtg.client;

import java.util.List;

public record TopDeckTournamentRequestDTO(
        String game,
        String format,
        int last,
        int participantMin,
        List<String> columns
) {
}
