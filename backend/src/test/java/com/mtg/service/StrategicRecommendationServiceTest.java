package com.mtg.service;

import com.mtg.domain.StrategicRecommendation;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.RecommendationParamsDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import com.mtg.service.meta.CommanderMetaProfile;
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
    com.mtg.service.meta.CommanderMetaProfileService commanderMetaProfileService = Mockito.mock(com.mtg.service.meta.CommanderMetaProfileService.class);

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
        sut.commanderMetaProfileService = commanderMetaProfileService;
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
        when(commanderMetaProfileService.findByCommanderAndBracket("Xenagos, God of Revels", "casual")).thenReturn(null);
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
        assertTrue(adds.stream().anyMatch(add -> Set.of("Greater Good", "Nature's Lore", "Heroic Intervention").contains(add)));
        assertFalse(adds.contains("Swords to Plowshares"));
        assertFalse(adds.contains("Arcane Encyclopedia"));
        recommendations.forEach(recommendation -> {
            assertFalse(recommendation.reasoning().isBlank());
            assertTrue(recommendation.reasoning().contains(recommendation.add()));
            assertTrue(recommendation.reasoning().contains(recommendation.remove()));
        });
    }

    @Test
    void shouldUseUsefulCommanderMetaProfileAsPriorityAndMentionMetaInReasoning() {
        Deck deck = xenagosDeck();
        CommanderMetaProfile profile = profile("Xenagos, God of Revels", "mid", 4, List.of(
                new MetaCard("Greater Good", 0.95, "draw", 4.0),
                new MetaCard("Nature's Lore", 0.30, "ramp", 2.0),
                new MetaCard("Heroic Intervention", 0.20, "protection", 2.0)
        ));

        when(deckRepository.findById(1L)).thenReturn(deck);
        when(commanderMetaProfileService.findByCommanderAndBracket("Xenagos, God of Revels", "mid")).thenReturn(profile);
        when(cardService.findByNames(Mockito.anyList())).thenReturn(xenagosCards());

        List<StrategicRecommendation> recommendations = sut.recommend(1L, new RecommendationParamsDTO(null, "mid", null, null));

        assertFalse(recommendations.isEmpty());
        assertEquals("Greater Good", recommendations.getFirst().add());
        assertEquals("meta_profile", recommendations.getFirst().source());
        assertEquals("mid", recommendations.getFirst().bracket());
        assertTrue(recommendations.getFirst().tags().contains("meta"));
        assertTrue(recommendations.getFirst().tags().contains("draw"));
        assertTrue(recommendations.getFirst().reasoning().contains("listas similares"));
        assertTrue(recommendations.getFirst().reasoning().contains("95%"));
    }

    @Test
    void shouldIgnoreMetaCardAlreadyInDeck() {
        Deck deck = xenagosDeck();
        deck.setCards(List.of(
                new DeckCard("Greater Good", 1),
                new DeckCard("Arcane Encyclopedia", 1),
                new DeckCard("Nissa's Pilgrimage", 1),
                new DeckCard("Colossal Dreadmaw", 1),
                new DeckCard("Forest", 35)
        ));
        CommanderMetaProfile profile = profile("Xenagos, God of Revels", "mid", 4, List.of(
                new MetaCard("Greater Good", 0.95, "draw", 4.0),
                new MetaCard("Nature's Lore", 0.80, "ramp", 2.0)
        ));

        when(deckRepository.findById(1L)).thenReturn(deck);
        when(commanderMetaProfileService.findByCommanderAndBracket("Xenagos, God of Revels", "mid")).thenReturn(profile);
        when(cardService.findByNames(Mockito.anyList())).thenReturn(xenagosCards());

        List<StrategicRecommendation> recommendations = sut.recommend(1L, new RecommendationParamsDTO(null, "mid", null, null));

        Set<String> adds = recommendations.stream().map(StrategicRecommendation::add).collect(java.util.stream.Collectors.toSet());
        assertFalse(adds.contains("Greater Good"));
        assertFalse(recommendations.isEmpty());
    }

    @Test
    void shouldRespectColorIdentityForMetaCandidates() {
        Deck deck = xenagosDeck();
        CommanderMetaProfile profile = profile("Xenagos, God of Revels", "mid", 4, List.of(
                new MetaCard("Swords to Plowshares", 0.99, "removal", 1.0),
                new MetaCard("Nature's Lore", 0.50, "ramp", 2.0)
        ));

        when(deckRepository.findById(1L)).thenReturn(deck);
        when(commanderMetaProfileService.findByCommanderAndBracket("Xenagos, God of Revels", "mid")).thenReturn(profile);
        when(cardService.findByNames(Mockito.anyList())).thenReturn(xenagosCards());

        List<StrategicRecommendation> recommendations = sut.recommend(1L, new RecommendationParamsDTO(null, "mid", null, null));

        Set<String> adds = recommendations.stream().map(StrategicRecommendation::add).collect(java.util.stream.Collectors.toSet());
        assertFalse(adds.contains("Swords to Plowshares"));
        assertFalse(recommendations.isEmpty());
    }

    @Test
    void shouldFallbackWithoutMetaReasoningWhenSampleSizeIsTooSmall() {
        Deck deck = xenagosDeck();
        CommanderMetaProfile smallProfile = profile("Xenagos, God of Revels", "mid", 2, List.of(
                new MetaCard("Greater Good", 1.0, "draw", 4.0)
        ));

        when(deckRepository.findById(1L)).thenReturn(deck);
        when(commanderMetaProfileService.findByCommanderAndBracket("Xenagos, God of Revels", "mid")).thenReturn(smallProfile);
        when(metaProvider.getTopCards("Xenagos, God of Revels")).thenReturn(List.of(
                new MetaCard("Nature's Lore", 0.80, "ramp", 2.0),
                new MetaCard("Heroic Intervention", 0.70, "protection", 2.0)
        ));
        when(cardService.findByNames(Mockito.anyList())).thenReturn(xenagosCards());

        List<StrategicRecommendation> recommendations = sut.recommend(1L, new RecommendationParamsDTO(null, "mid", null, null));

        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.stream().noneMatch(recommendation -> recommendation.reasoning().contains("listas similares")));
        assertTrue(recommendations.stream().allMatch(recommendation -> recommendation.source().equals("heuristic_fallback")));
    }

    @Test
    void shouldLimitRecommendationsToFive() {
        Deck deck = xenagosDeck();
        CommanderMetaProfile profile = profile("Xenagos, God of Revels", "mid", 4, List.of(
                new MetaCard("Greater Good", 0.95, "draw", 4.0),
                new MetaCard("Nature's Lore", 0.90, "ramp", 2.0),
                new MetaCard("Heroic Intervention", 0.85, "protection", 2.0),
                new MetaCard("Beast Within", 0.80, "removal", 3.0),
                new MetaCard("Overwhelming Stampede", 0.75, "finisher", 5.0),
                new MetaCard("Arcane Signet", 0.70, "ramp", 2.0)
        ));

        when(deckRepository.findById(1L)).thenReturn(deck);
        when(commanderMetaProfileService.findByCommanderAndBracket("Xenagos, God of Revels", "mid")).thenReturn(profile);
        when(cardService.findByNames(Mockito.anyList())).thenReturn(xenagosCards());

        List<StrategicRecommendation> recommendations = sut.recommend(1L, new RecommendationParamsDTO(null, "mid", null, null, null, 99));

        assertTrue(recommendations.size() <= 5);
        recommendations.forEach(recommendation -> {
            assertFalse(recommendation.add().isBlank());
            assertFalse(recommendation.remove().isBlank());
        });
    }

    @Test
    void shouldStillRecommendForCedhWhenCardMetadataIsUnavailable() {
        Deck deck = xenagosDeck();

        when(deckRepository.findById(1L)).thenReturn(deck);
        when(commanderMetaProfileService.findByCommanderAndBracket("Xenagos, God of Revels", "cedh")).thenReturn(null);
        when(metaProvider.getTopCards("Xenagos, God of Revels")).thenReturn(List.of());
        when(cardService.findByNames(Mockito.anyList())).thenReturn(Map.of());

        List<StrategicRecommendation> recommendations = sut.recommend(1L, new RecommendationParamsDTO(null, "cedh", null, null));

        assertFalse(recommendations.isEmpty());
        assertTrue(recommendations.size() >= 3);
        assertTrue(recommendations.stream().allMatch(recommendation -> recommendation.bracket().equals("cedh")));
        assertTrue(recommendations.stream().noneMatch(recommendation -> recommendation.remove().equals("Forest")));
        recommendations.forEach(recommendation -> {
            assertFalse(recommendation.add().isBlank());
            assertFalse(recommendation.remove().isBlank());
        });
    }

    private static Map.Entry<String, CardResponseDTO> entry(CardResponseDTO card) {
        return Map.entry(card.name().toLowerCase(), card);
    }

    private static Deck xenagosDeck() {
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
        return deck;
    }

    private static CommanderMetaProfile profile(String commander, String bracket, int sampleSize, List<MetaCard> cards) {
        return new CommanderMetaProfile(
                commander,
                bracket,
                "local_only",
                sampleSize,
                cards,
                Map.of(),
                List.of(),
                List.of("LOCAL"),
                java.time.OffsetDateTime.now()
        );
    }

    private static Map<String, CardResponseDTO> xenagosCards() {
        return Map.ofEntries(
                entry(card("Xenagos, God of Revels", "{3}{R}{G}", "Legendary Enchantment Creature - God", "At the beginning of combat, target creature gains haste and trample and gets +X/+X.", 5.0, List.of("R", "G"))),
                entry(card("Arcane Encyclopedia", "{3}", "Artifact", "{3}, {T}: Draw a card.", 3.0, List.of())),
                entry(card("Nissa's Pilgrimage", "{2}{G}", "Sorcery", "Search your library for up to two basic Forest cards.", 3.0, List.of("G"))),
                entry(card("Colossal Dreadmaw", "{4}{G}{G}", "Creature - Dinosaur", "Trample.", 6.0, List.of("G"))),
                entry(card("Rhonas's Monument", "{3}", "Legendary Artifact", "Green creature spells you cast cost {1} less. Target creature gets +2/+2 and gains trample.", 3.0, List.of())),
                entry(card("Forest", "", "Basic Land - Forest", "Add {G}.", 0.0, List.of("G"))),
                entry(card("Greater Good", "{2}{G}{G}", "Enchantment", "Sacrifice a creature: Draw cards equal to the sacrificed creature's power.", 4.0, List.of("G"))),
                entry(card("Nature's Lore", "{1}{G}", "Sorcery", "Search your library for a Forest card and put it onto the battlefield.", 2.0, List.of("G"))),
                entry(card("Heroic Intervention", "{1}{G}", "Instant", "Permanents you control gain hexproof and indestructible until end of turn.", 2.0, List.of("G"))),
                entry(card("Swords to Plowshares", "{W}", "Instant", "Exile target creature.", 1.0, List.of("W"))),
                entry(card("Beast Within", "{2}{G}", "Instant", "Destroy target permanent.", 3.0, List.of("G"))),
                entry(card("Overwhelming Stampede", "{3}{G}{G}", "Sorcery", "Creatures you control gain trample and get +X/+X until end of turn.", 5.0, List.of("G"))),
                entry(card("Arcane Signet", "{2}", "Artifact", "Add one mana of any color in your commander's color identity.", 2.0, List.of()))
        );
    }

    private static CardResponseDTO card(String name, String manaCost, String typeLine, String oracle, Double cmc, List<String> colors) {
        return new CardResponseDTO(name, manaCost, typeLine, oracle, cmc, colors, List.of());
    }
}
