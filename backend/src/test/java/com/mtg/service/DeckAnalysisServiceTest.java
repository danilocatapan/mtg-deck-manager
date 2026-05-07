package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

        when(cardService.findByNames(List.of("Sol Ring", "Opt"))).thenReturn(Map.of(
                "sol ring", new CardResponseDTO("Sol Ring","{1}","Artifact","{T}: Add {C}{C}.",1.0, java.util.List.of(), java.util.List.of()),
                "opt", new CardResponseDTO("Opt","{U}","Instant","Draw a card.",1.0, java.util.List.of(), java.util.List.of())
        ));
        when(cardService.normalizeLookupName("Sol Ring")).thenReturn("sol ring");
        when(cardService.normalizeLookupName("Opt")).thenReturn("opt");

        var analysis = sut.analyzeDeck(1L);

        assertEquals(3, analysis.totalCards()); // 1 + 2
        // average cmc = (1*1 + 1*2)/3 = 1.0
        assertEquals(1.0, analysis.averageCmc(), 0.0001);
        assertEquals(1, analysis.rampCount());
        assertEquals(2, analysis.drawCount());
        verify(cardService).findByNames(List.of("Sol Ring", "Opt"));
        verify(cardService, never()).searchByName("Sol Ring");
        verify(cardService, never()).searchByName("Opt");
    }

    @Test
    void calculatesAdvancedDiagnostics() {
        Deck deck = new Deck();
        deck.setId(2L);
        deck.setCards(List.of(
                new DeckCard("Forest", 35),
                new DeckCard("Basalt Monolith", 1),
                new DeckCard("Rings of Brighthearth", 1),
                new DeckCard("Heroic Intervention", 1),
                new DeckCard("Blasphemous Act", 1),
                new DeckCard("Beast Within", 1)
        ));

        when(deckRepository.findById(2L)).thenReturn(deck);
        when(cardService.findByNames(List.of("Forest", "Basalt Monolith", "Rings of Brighthearth", "Heroic Intervention", "Blasphemous Act", "Beast Within"))).thenReturn(Map.of(
                "forest", new CardResponseDTO("Forest", "", "Basic Land - Forest", "Add {G}.", 0.0, java.util.List.of("G"), java.util.List.of()),
                "basalt monolith", new CardResponseDTO("Basalt Monolith", "{3}", "Artifact", "{T}: Add {C}{C}{C}.", 3.0, java.util.List.of(), java.util.List.of()),
                "rings of brighthearth", new CardResponseDTO("Rings of Brighthearth", "{3}", "Artifact", "Whenever you activate an ability, you may pay {2}. If you do, copy that ability.", 3.0, java.util.List.of(), java.util.List.of()),
                "heroic intervention", new CardResponseDTO("Heroic Intervention", "{1}{G}", "Instant", "Permanents you control gain hexproof and indestructible until end of turn.", 2.0, java.util.List.of("G"), java.util.List.of()),
                "blasphemous act", new CardResponseDTO("Blasphemous Act", "{8}{R}", "Sorcery", "This spell deals 13 damage to each creature.", 9.0, java.util.List.of("R"), java.util.List.of()),
                "beast within", new CardResponseDTO("Beast Within", "{2}{G}", "Instant", "Destroy target permanent.", 3.0, java.util.List.of("G"), java.util.List.of())
        ));
        when(cardService.normalizeLookupName(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class).toLowerCase());

        var analysis = sut.analyzeDeck(2L);

        assertEquals(40, analysis.totalCards());
        assertEquals(35, analysis.manaBase().landCount());
        assertEquals(1, analysis.boardWipeCount());
        assertEquals(1, analysis.protectionCount());
        assertEquals(1, analysis.combos().present().size());
        assertEquals("Basalt Monolith + Rings of Brighthearth", analysis.combos().present().getFirst().name());
        assertTrue(analysis.probabilities().openingHandTwoPlusLands() > 0.9);
        assertTrue(analysis.score().bracketPressure() > 0);
    }
}
