package com.mtg.domain;

public record MetaComparison(
        String role,
        int deckCount,
        int targetCount,
        int percentileBehind,
        String message
) {
}
