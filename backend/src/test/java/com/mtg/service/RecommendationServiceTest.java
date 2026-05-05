package com.mtg.service;

import com.mtg.domain.DeckRecommendations;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.RecommendationParamsDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    DeckRepository deckRepository = Mockito.mock(DeckRepository.class);
    DeckAnalysisService analysisService = Mockito.mock(DeckAnalysisService.class);
    CardService cardService = Mockito.mock(CardService.class);
    DeckCompleter deckCompleter = Mockito.mock(DeckCompleter.class);
    com.mtg.service.meta.MetaProvider metaProvider = Mockito.mock(com.mtg.service.meta.MetaProvider.class);
    com.mtg.service.synergy.SynergyEngine synergyEngine = Mockito.mock(com.mtg.service.synergy.SynergyEngine.class);

    RecommendationService sut;

    @BeforeEach
    void setup() {
        sut = new RecommendationService();
        sut.deckRepository = deckRepository;
        sut.deckAnalysisService = analysisService;
        sut.cardService = cardService;
        sut.metaProvider = metaProvider;
        sut.synergyEngine = synergyEngine;
        sut.deckCompleter = deckCompleter;

        // default metaProvider and synergyEngine behavior for unit tests
        when(metaProvider.getTopCards(org.mockito.ArgumentMatchers.anyString())).thenReturn(java.util.List.of());
        when(synergyEngine.tagsForCard(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.Set.of());
        when(synergyEngine.computeSynergy(org.mockito.ArgumentMatchers.anySet(), org.mockito.ArgumentMatchers.anySet(), org.mockito.ArgumentMatchers.anySet())).thenReturn(0.0);
        when(deckCompleter.complete(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(java.util.List.of());
    }

    @Test
    void basicRecommendationFlow() {
        Deck deck = new Deck();
        deck.setId(1L);
        deck.setCards(List.of(new DeckCard("Sol Ring",1)));

        when(deckRepository.findById(1L)).thenReturn(deck);

        // analysis with gaps
        var analysis = new com.mtg.domain.DeckAnalysis(1.0,1,0,0,0,java.util.Map.of());
        when(analysisService.analyzeDeck(1L)).thenReturn(analysis);

        // cardService query returns some candidates
        when(cardService.searchByQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(new CardResponseDTO("Arcane Signet","{2}","Artifact","{T}: Add {C}",2.0, java.util.List.of(), java.util.List.of())));
        when(cardService.searchByName(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(new CardResponseDTO("Sol Ring","{1}","Artifact","{T}: Add {C}{C}.",1.0, java.util.List.of(), java.util.List.of())));

        DeckRecommendations recs = sut.recommend(1L, new RecommendationParamsDTO(200.0,"casual","aggro",null));
        assertNotNull(recs);
    }

    @Test
    void shouldAggregateBaselineLandRecommendations() {
        Deck deck = new Deck();
        deck.setId(1L);
        deck.setColorIdentity("RG");
        deck.setCards(List.of(new DeckCard("Sol Ring", 1)));

        when(deckRepository.findById(1L)).thenReturn(deck);
        when(analysisService.analyzeDeck(1L)).thenReturn(new com.mtg.domain.DeckAnalysis(1.0, 1, 0, 0, 0, java.util.Map.of()));
        when(cardService.searchByQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        when(cardService.searchByName(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(new CardResponseDTO("Sol Ring", "{1}", "Artifact", "{T}: Add {C}{C}.", 1.0, java.util.List.of(), java.util.List.of())));

        DeckRecommendations recs = sut.recommend(1L, new RecommendationParamsDTO(200.0, "casual", "aggro", null));

        assertEquals(2, recs.add().size());
        assertEquals(18, recs.add().stream().filter(item -> item.name().equals("Mountain")).findFirst().orElseThrow().quantity());
        assertEquals(18, recs.add().stream().filter(item -> item.name().equals("Forest")).findFirst().orElseThrow().quantity());
    }
}
