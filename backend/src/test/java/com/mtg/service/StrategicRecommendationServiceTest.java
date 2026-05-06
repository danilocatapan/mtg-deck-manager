package com.mtg.service;

import com.mtg.domain.StrategicRecommendation;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.RecommendationParamsDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import com.mtg.service.meta.MetaCard;
import com.mtg.service.meta.MetaProvider;
import com.mtg.service.synergy.SynergyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class StrategicRecommendationServiceTest {

    DeckRepository deckRepository = Mockito.mock(DeckRepository.class);
    CardService cardService = Mockito.mock(CardService.class);
    MetaProvider metaProvider = Mockito.mock(MetaProvider.class);

    StrategicRecommendationService sut;

    @BeforeEach
    void setup() {
        SynergyEngine synergyEngine = Mockito.mock(SynergyEngine.class);
        when(synergyEngine.tagsForCard(Mockito.any())).thenReturn(Set.of("combat"));
        when(synergyEngine.aggregateTags(Mockito.anyList())).thenReturn(Set.of("combat", "ramp"));
        when(synergyEngine.computeSynergy(Mockito.anySet(), Mockito.anySet(), Mockito.anySet())).thenReturn(0.5);

        DeckRoleAnalyzer roleAnalyzer = new DeckRoleAnalyzer();
        roleAnalyzer.synergyEngine = synergyEngine;

        CommanderArchetypeDetector archetypeDetector = new CommanderArchetypeDetector();
        archetypeDetector.synergyEngine = synergyEngine;

        CandidateAddSelector addSelector = new CandidateAddSelector();
        addSelector.cardService = cardService;
        addSelector.synergyEngine = synergyEngine;

        CandidateCutSelector cutSelector = new CandidateCutSelector();
        cutSelector.synergyEngine = synergyEngine;

        RecommendationPairer pairer = new RecommendationPairer();
        pairer.reasoningBuilder = new RecommendationReasoningBuilder();

        sut = new StrategicRecommendationService();
        sut.deckRepository = deckRepository;
        sut.cardService = cardService;
        sut.metaProvider = metaProvider;
        sut.deckRoleAnalyzer = roleAnalyzer;
        sut.archetypeDetector = archetypeDetector;
        sut.addSelector = addSelector;
        sut.cutSelector = cutSelector;
        sut.pairer = pairer;
    }

    @Test
    void shouldReturnStrategicJsonShapeRecommendationsRespectingCommanderRules() {
        Deck deck = new Deck();
        deck.setId(1L);
        deck.setCommander("Xenagos, God of Revels");
        deck.setColorIdentity("RG");
        deck.setCards(List.of(
                new DeckCard("Arcane Encyclopedia", 1),
                new DeckCard("Nissa's Pilgrimage", 1),
                new DeckCard("Colossal Dreadmaw", 1),
                new DeckCard("Rhonas's Monument", 1),
                new DeckCard("Forest", 35)
        ));

        List<MetaCard> metaCards = List.of(
                new MetaCard("Greater Good", 0.80, "draw", 4.0),
                new MetaCard("Nature's Lore", 0.75, "ramp", 2.0),
                new MetaCard("Heroic Intervention", 0.70, "protection", 2.0),
                new MetaCard("Swords to Plowshares", 0.90, "removal", 1.0)
        );

        when(deckRepository.findById(1L)).thenReturn(deck);
        when(metaProvider.getTopCards("Xenagos, God of Revels")).thenReturn(metaCards);
        when(cardService.findByNames(Mockito.anyList())).thenReturn(Map.ofEntries(
                entry(card("Xenagos, God of Revels", "{3}{R}{G}", "Legendary Enchantment Creature - God", "At the beginning of combat, target creature gains haste and trample and gets +X/+X.", 5.0, List.of("R", "G"))),
                entry(card("Arcane Encyclopedia", "{3}", "Artifact", "{3}, {T}: Draw a card.", 3.0, List.of())),
                entry(card("Nissa's Pilgrimage", "{2}{G}", "Sorcery", "Search your library for up to two basic Forest cards.", 3.0, List.of("G"))),
                entry(card("Colossal Dreadmaw", "{4}{G}{G}", "Creature - Dinosaur", "Trample.", 6.0, List.of("G"))),
                entry(card("Rhonas's Monument", "{3}", "Legendary Artifact", "Green creature spells you cast cost {1} less. Target creature gets +2/+2 and gains trample.", 3.0, List.of())),
                entry(card("Forest", "", "Basic Land - Forest", "Add {G}.", 0.0, List.of("G"))),
                entry(card("Greater Good", "{2}{G}{G}", "Enchantment", "Sacrifice a creature: Draw cards equal to the sacrificed creature's power.", 4.0, List.of("G"))),
                entry(card("Nature's Lore", "{1}{G}", "Sorcery", "Search your library for a Forest card and put it onto the battlefield.", 2.0, List.of("G"))),
                entry(card("Heroic Intervention", "{1}{G}", "Instant", "Permanents you control gain hexproof and indestructible until end of turn.", 2.0, List.of("G"))),
                entry(card("Swords to Plowshares", "{W}", "Instant", "Exile target creature.", 1.0, List.of("W")))
        ));

        List<StrategicRecommendation> recommendations = sut.recommend(1L, new RecommendationParamsDTO(null, "casual", null, null));

        assertEquals(3, recommendations.size());
        Set<String> adds = recommendations.stream().map(StrategicRecommendation::add).collect(java.util.stream.Collectors.toSet());
        assertTrue(adds.contains("Greater Good"));
        assertTrue(adds.contains("Nature's Lore"));
        assertTrue(adds.contains("Heroic Intervention"));
        assertFalse(adds.contains("Swords to Plowshares"));
        assertFalse(adds.contains("Arcane Encyclopedia"));
        recommendations.forEach(recommendation -> {
            assertFalse(recommendation.reasoning().isBlank());
            assertTrue(recommendation.reasoning().contains(recommendation.add()));
            assertTrue(recommendation.reasoning().contains(recommendation.remove()));
        });
    }

    private static Map.Entry<String, CardResponseDTO> entry(CardResponseDTO card) {
        return Map.entry(card.name().toLowerCase(), card);
    }

    private static CardResponseDTO card(String name, String manaCost, String typeLine, String oracle, Double cmc, List<String> colors) {
        return new CardResponseDTO(name, manaCost, typeLine, oracle, cmc, colors, List.of());
    }
}
