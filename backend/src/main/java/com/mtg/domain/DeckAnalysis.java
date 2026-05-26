package com.mtg.domain;

import java.util.List;
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
        ExplainableScore score,
        Map<String, Integer> cardTags,
        Map<Integer, List<RoleCard>> manaCurveCards,
        Map<String, List<RoleCard>> roleCards
) {
    public DeckAnalysis {
        manaCurve = manaCurve == null ? Map.of() : Map.copyOf(manaCurve);
        roles = roles == null ? Map.of() : Map.copyOf(roles);
        manaCurveByType = manaCurveByType == null ? Map.of() : Map.copyOf(manaCurveByType);
        cardTags = cardTags == null ? Map.of() : Map.copyOf(cardTags);
        manaCurveCards = copyCardMap(manaCurveCards);
        roleCards = copyCardMap(roleCards);
    }

    public DeckAnalysis(
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
        this(
                averageCmc,
                totalCards,
                rampCount,
                drawCount,
                removalCount,
                manaCurve,
                roles,
                manaBase,
                manaCurveByType,
                earlyGameCount,
                interactionCount,
                boardWipeCount,
                protectionCount,
                winconCount,
                combos,
                probabilities,
                score,
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

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
                ExplainableScore.empty(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    private static <K> Map<K, List<RoleCard>> copyCardMap(Map<K, List<RoleCard>> cardsByGroup) {
        if (cardsByGroup == null || cardsByGroup.isEmpty()) {
            return Map.of();
        }
        return cardsByGroup.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() == null ? List.of() : List.copyOf(entry.getValue())
                ));
    }
}
