package com.mtg.service;

import com.mtg.domain.StrategicRecommendation;
import com.mtg.dto.CardResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationPairerCompatibilityTest {

    RecommendationPairer pairer;
    CommanderArchetypeProfile profile;
    DeckRoleSummary roles;

    @BeforeEach
    void setup() {
        pairer = new RecommendationPairer();
        pairer.reasoningBuilder = new RecommendationReasoningBuilder();
        profile = new CommanderArchetypeProfile("Xenagos, God of Revels", Set.of("R", "G"), "combat", "combat damage", Set.of("combat"));
        roles = new DeckRoleSummary(99, 35, 12, 8, 7, 4, 0, 5, 3.8, java.util.Map.of("ramp", 2), Set.of("combat"));
    }

    @Test
    void rampCannotCutSynergisticFinisherWhenNoCompatibleCutExists() {
        List<StrategicRecommendation> recommendations = pairer.pair(
                List.of(candidate("Nature's Lore", "ramp", 0.9, "melhora ramp")),
                List.of(candidate("Atarka, World Render", "finisher", 0.8, "finisher sinergico")),
                profile,
                roles,
                5,
                "cedh"
        );

        assertTrue(recommendations.isEmpty());
    }

    @Test
    void rampPrefersSlowRampOverSynergisticFinisher() {
        List<StrategicRecommendation> recommendations = pairer.pair(
                List.of(candidate("Nature's Lore", "ramp", 0.9, "melhora ramp")),
                List.of(
                        candidate("Atarka, World Render", "finisher", 0.8, "finisher sinergico"),
                        candidate("Cultivate", "ramp", 0.7, "ramp lento")
                ),
                profile,
                roles,
                5,
                "cedh"
        );

        assertEquals(1, recommendations.size());
        assertEquals("Nature's Lore", recommendations.getFirst().add());
        assertEquals("Cultivate", recommendations.getFirst().remove());
    }

    private StrategicCandidate candidate(String name, String role, double score, String reason) {
        return new StrategicCandidate(card(name), role, score, reason, false, 0.0, 0.5, "test");
    }

    private CardResponseDTO card(String name) {
        return switch (name) {
            case "Atarka, World Render" -> new CardResponseDTO(name, "{5}{R}{G}", "Legendary Creature - Dragon", "Flying, trample. Whenever a Dragon you control attacks, it gains double strike until end of turn.", 7.0, List.of("R", "G"), List.of());
            case "Nature's Lore" -> new CardResponseDTO(name, "{1}{G}", "Sorcery", "Search your library for a Forest card and put it onto the battlefield.", 2.0, List.of("G"), List.of());
            case "Cultivate" -> new CardResponseDTO(name, "{2}{G}", "Sorcery", "Search your library for basic land cards.", 3.0, List.of("G"), List.of());
            default -> new CardResponseDTO(name, "{3}", "Spell", "", 3.0, List.of(), List.of());
        };
    }
}
