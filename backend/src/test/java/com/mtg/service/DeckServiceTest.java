package com.mtg.service;

import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.DeckRequestDTO;
import com.mtg.dto.DeckResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    private static final String OWNER_ID = "google-user-1";

    @Mock
    DeckRepository deckRepository;

    @Mock
    DeckImportService importService;

    @InjectMocks
    DeckService deckService;

    @BeforeEach
    void setup() {
        deckService.importService = importService;
    }

    @Test
    void createDeck_success() {
        DeckRequestDTO request = new DeckRequestDTO("My Deck", "Commander", List.of(new DeckCardDTO("Sol Ring",1)));
        doAnswer(invocation -> {
            Deck d = invocation.getArgument(0);
            d.setId(1L);
            return null;
        }).when(deckRepository).persist(any(Deck.class));

        DeckResponseDTO resp = deckService.createDeck(request, OWNER_ID);

        assertNotNull(resp.id());
        assertEquals("My Deck", resp.name());
        verify(deckRepository).persist(argThat(deck -> OWNER_ID.equals(deck.getOwnerId())));
    }

    @Test
    void createDeck_validationFails() {
        DeckRequestDTO request = new DeckRequestDTO(" ", "Commander", List.of());
        assertThrows(IllegalArgumentException.class, () -> deckService.createDeck(request, OWNER_ID));
    }

    @Test
    void updateDeck_notFound() {
        Long id = 999L;
        when(deckRepository.findByIdAndOwner(id, OWNER_ID)).thenReturn(null);
        DeckRequestDTO request = new DeckRequestDTO("Name","Cmd", List.of(new DeckCardDTO("Sol Ring",1)));
        assertNull(deckService.updateDeck(id, request, OWNER_ID));
    }

    @Test
    void exportDeck_withCards() {
        Deck deck = new Deck("My Deck", "Cmd", List.of(new DeckCard("Sol Ring",1), new DeckCard("Command Tower",1)));
        when(deckRepository.findByIdAndOwner(1L, OWNER_ID)).thenReturn(deck);

        String exported = deckService.exportDeck(1L, OWNER_ID);

        assertEquals("1 Sol Ring\n1 Command Tower", exported);
    }

    @Test
    void exportDeck_empty() {
        Deck deck = new Deck("Empty", "Cmd", List.of());
        when(deckRepository.findByIdAndOwner(2L, OWNER_ID)).thenReturn(deck);

        String exported = deckService.exportDeck(2L, OWNER_ID);

        assertEquals("", exported);
    }

    @Test
    void exportDeck_notFound() {
        when(deckRepository.findByIdAndOwner(999L, OWNER_ID)).thenReturn(null);
        assertNull(deckService.exportDeck(999L, OWNER_ID));
    }

    @Test
    void importDeck_reportsActualCardCountWhenTooLarge() {
        when(importService.parse(any())).thenReturn(List.of(
                new DeckCard("Mountain", 60),
                new DeckCard("Forest", 45)
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> deckService.importDeck(new com.mtg.dto.DeckImportDTO("Big Deck", "Cmd", "60 Mountain\n45 Forest"), OWNER_ID)
        );

        assertEquals("Imported deck has 105 cards; maximum is 99.", exception.getMessage());
    }

    @Test
    void createDeck_requiresOwner() {
        DeckRequestDTO request = new DeckRequestDTO("My Deck", "Commander", List.of(new DeckCardDTO("Sol Ring",1)));

        assertThrows(IllegalArgumentException.class, () -> deckService.createDeck(request, " "));
        verify(deckRepository, never()).persist(any(Deck.class));
    }
}
