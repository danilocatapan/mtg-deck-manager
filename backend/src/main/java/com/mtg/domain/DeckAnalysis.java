package com.mtg.domain;

import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "DeckAnalysis")
public record DeckAnalysis(
        double averageCmc,
        int totalCards,
        int rampCount,
        int drawCount,
        int removalCount,
        Map<Integer, Integer> manaCurve
) {
}
