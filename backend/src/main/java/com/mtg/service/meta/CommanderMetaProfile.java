package com.mtg.service.meta;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record CommanderMetaProfile(
        String commander,
        String bracket,
        String sourceMode,
        int sampleSize,
        List<MetaCard> topCards,
        Map<String, Integer> roleTargets,
        List<String> commonArchetypes,
        List<String> sourcesUsed,
        OffsetDateTime updatedAt
) {
    public CommanderMetaProfile {
        topCards = topCards == null ? List.of() : List.copyOf(topCards);
        roleTargets = roleTargets == null ? Map.of() : Map.copyOf(roleTargets);
        commonArchetypes = commonArchetypes == null ? List.of() : List.copyOf(commonArchetypes);
        sourcesUsed = sourcesUsed == null ? List.of() : List.copyOf(sourcesUsed);
        updatedAt = updatedAt == null ? OffsetDateTime.now() : updatedAt;
    }

    public static CommanderMetaProfile empty(String commander, String bracket, String sourceMode) {
        String safeBracket = bracket == null || bracket.isBlank() ? "casual" : bracket;
        String safeSourceMode = sourceMode == null || sourceMode.isBlank() ? "auto" : sourceMode;
        return new CommanderMetaProfile(
                commander,
                safeBracket,
                safeSourceMode,
                0,
                List.of(),
                RoleTargets.forBracket(safeBracket).asMap(),
                List.of(),
                List.of("LOCAL"),
                OffsetDateTime.now()
        );
    }
}
