package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class DeckAnalysisServiceTest {

    DeckRepository deckRepository = Mockito.mock(DeckRepository.class);
    CardService cardService = Mockito.mock(CardService.class);
    ClassificationService classificationService = new ClassificationService();

    DeckAnalysisService sut;

    @BeforeEach
    void setup() {
        sut = new DeckAnalysisService(deckRepository, cardService, classificationService);
    }

    @Test
    void calculatesCmcAverageAndCounts() {
        Deck deck = new Deck();
        deck.setId(1L);
        DeckCard c1 = new DeckCard("Sol Ring", 1);
        DeckCard c2 = new DeckCard("Opt", 2);
        deck.setCards(List.of(c1, c2));

        when(deckRepository.findById(1L)).thenReturn(deck);

        when(cardService.searchByName("Sol Ring")).thenReturn(List.of(new CardResponseDTO("Sol Ring","{1}","Artifact","{T}: Add {C}{C}.",1.0, java.util.List.of(), java.util.List.of())));
        when(cardService.searchByName("Opt")).thenReturn(List.of(new CardResponseDTO("Opt","{U}","Instant","Draw a card.",1.0, java.util.List.of(), java.util.List.of())));

        var analysis = sut.analyzeDeck(1L);

        assertEquals(3, analysis.totalCards()); // 1 + 2
        // average cmc = (1*1 + 1*2)/3 = 1.0
        assertEquals(1.0, analysis.averageCmc(), 0.0001);
        assertEquals(1, analysis.rampCount());
        assertEquals(2, analysis.drawCount());
    }
}
