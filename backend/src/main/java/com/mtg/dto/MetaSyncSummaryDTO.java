package com.mtg.dto;

import com.mtg.service.meta.MetaSourceStatus;

import java.util.List;
import java.util.Map;

public record MetaSyncSummaryDTO(
        String status,
        int importedDecks,
        int discardedDecks,
        int snapshotDecks,
        int commandersCovered,
        Map<String, Integer> coverageByBracket,
        int profilesBuilt,
        List<String> errors,
        List<String> limitations,
        MetaSourceStatus source
) {
    public MetaSyncSummaryDTO {
        coverageByBracket = coverageByBracket == null ? Map.of() : Map.copyOf(coverageByBracket);
        errors = errors == null ? List.of() : List.copyOf(errors);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }
}
