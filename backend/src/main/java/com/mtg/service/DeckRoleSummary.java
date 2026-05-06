package com.mtg.service;

import java.util.Map;
import java.util.Set;

record DeckRoleSummary(
        int totalCards,
        int lands,
        int ramp,
        int draw,
        int removal,
        int protection,
        int boardWipes,
        int finishers,
        double averageCmc,
        Map<String, Integer> gaps,
        Set<String> deckTags
) {
}
