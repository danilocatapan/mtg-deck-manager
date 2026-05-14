package com.mtg.service;

import com.mtg.domain.ComboAnalysis;
import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.service.synergy.SynergyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CandidateAddSelectorArchetypeTest {

    CandidateAddSelector selector;

    @BeforeEach
    void setup() {
        SynergyEngine synergyEngine = Mockito.mock(SynergyEngine.class);
        when(synergyEngine.tagsForCard(Mockito.any())).thenReturn(Set.of("token"));
        when(synergyEngine.computeSynergy(Mockito.anySet(), Mockito.anySet(), Mockito.anySet())).thenReturn(0.6);

        ComboDetectionService comboDetectionService = Mockito.mock(ComboDetectionService.class);
        when(comboDetectionService.completionSignals(Mockito.anySet())).thenReturn(List.of());

        selector = new CandidateAddSelector();
        selector.synergyEngine = synergyEngine;
        selector.roleClassifier = new CardRoleClassifier();
        selector.comboDetectionService = comboDetectionService;
    }

    @Test
    void tokenArchetypeReceivesTokenSpecificCandidates() {
        List<StrategicCandidate> candidates = selector.select(
                deck("Rhys the Redeemed", "WG"),
                List.of(),
                new java.util.HashMap<>(),
                profile("tokens", "W", "G"),
                roles(Set.of("token"), Map.of("draw", 2, "finisher", 2)),
                "mid",
                false,
                "consistency",
                null,
                Set.of(),
                StrategicDeckAssessment.empty()
        );

        Set<String> adds = names(candidates);
        assertTrue(adds.contains("Skullclamp") || adds.contains("Beastmaster Ascension") || adds.contains("Tendershoot Dryad"));
    }

    @Test
    void reanimatorArchetypeReceivesGraveyardSpecificCandidates() {
        List<StrategicCandidate> candidates = selector.select(
                deck("Meren of Clan Nel Toth", "BG"),
                List.of(),
                new java.util.HashMap<>(),
                profile("reanimator", "B", "G"),
                roles(Set.of("graveyard", "recursion"), Map.of("tutor", 1, "finisher", 1)),
                "high-power",
                false,
                "competitive",
                null,
                Set.of(),
                StrategicDeckAssessment.empty()
        );

        Set<String> adds = names(candidates);
        assertTrue(adds.contains("Reanimate") || adds.contains("Animate Dead") || adds.contains("Entomb"));
    }

    @Test
    void rejectsFunctionalManaOutsideCommanderColors() {
        Map<String, CardResponseDTO> knownCards = new java.util.HashMap<>();
        knownCards.put("marble diamond", card("Marble Diamond", "{2}", "Artifact", "Marble Diamond enters the battlefield tapped. Add {W}.", 2.0));
        knownCards.put("sky diamond", card("Sky Diamond", "{2}", "Artifact", "Sky Diamond enters the battlefield tapped. Add {U}.", 2.0));
        knownCards.put("mind stone", card("Mind Stone", "{2}", "Artifact", "{T}: Add {C}.", 2.0));

        List<StrategicCandidate> candidates = selector.select(
                deck("Xenagos, God of Revels", "RG"),
                List.of(
                        new com.mtg.service.meta.MetaCard("Marble Diamond", 0.9, "ramp", 2.0),
                        new com.mtg.service.meta.MetaCard("Sky Diamond", 0.8, "ramp", 2.0),
                        new com.mtg.service.meta.MetaCard("Mind Stone", 0.7, "ramp", 2.0)
                ),
                knownCards,
                profile("combat", "R", "G"),
                roles(Set.of("combat", "ramp"), Map.of("ramp", 2)),
                "cedh",
                true,
                "consistency",
                null,
                Set.of(),
                StrategicDeckAssessment.empty()
        );

        Set<String> adds = names(candidates);
        assertFalse(adds.contains("Marble Diamond"));
        assertFalse(adds.contains("Sky Diamond"));
        assertTrue(adds.contains("Mind Stone") || adds.contains("Sol Ring") || adds.contains("Arcane Signet"));
    }

    private Deck deck(String commander, String colors) {
        Deck deck = new Deck();
        deck.setCommander(commander);
        deck.setColorIdentity(colors);
        deck.setCards(List.of(
                new DeckCard("Slow Value Card", 1),
                new DeckCard("Forest", 20),
                new DeckCard("Swamp", 10)
        ));
        return deck;
    }

    private CommanderArchetypeProfile profile(String archetype, String... colors) {
        return new CommanderArchetypeProfile("fixture", Set.of(colors), archetype, "fixture plan", Set.of(archetype));
    }

    private DeckRoleSummary roles(Set<String> tags, Map<String, Integer> gaps) {
        return new DeckRoleSummary(31, 30, 5, 4, 3, 1, 0, 2, 3.8, gaps, tags);
    }

    private CardResponseDTO card(String name, String manaCost, String typeLine, String oracle, Double cmc, String... colors) {
        return new CardResponseDTO(name, manaCost, typeLine, oracle, cmc, List.of(colors), List.of());
    }

    private Set<String> names(List<StrategicCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> candidate.card().name())
                .collect(java.util.stream.Collectors.toSet());
    }
}
