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
        Map<Integer, Integer> manaCurve,
        Map<String, Integer> roles,
        ManaBaseAnalysis manaBase,
        Map<String, Map<Integer, Integer>> manaCurveByType,
        int earlyGameCount,
        int interactionCount,
        int boardWipeCount,
        int protectionCount,
        int winconCount,
        ComboAnalysis combos,
        ProbabilityAnalysis probabilities,
        ExplainableScore score
) {
    public DeckAnalysis(
            double averageCmc,
            int totalCards,
            int rampCount,
            int drawCount,
            int removalCount,
            Map<Integer, Integer> manaCurve
    ) {
        this(
                averageCmc,
                totalCards,
                rampCount,
                drawCount,
                removalCount,
                manaCurve,
                Map.of(),
                ManaBaseAnalysis.empty(),
                Map.of(),
                0,
                removalCount,
                0,
                0,
                0,
                ComboAnalysis.empty(),
                ProbabilityAnalysis.empty(),
                ExplainableScore.empty()
        );
    }
}
