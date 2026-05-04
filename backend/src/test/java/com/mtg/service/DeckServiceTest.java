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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    DeckRepository deckRepository;

    @InjectMocks
    DeckService deckService;

    @Test
    void createDeck_success() {
        DeckRequestDTO request = new DeckRequestDTO("My Deck", "Commander", List.of(new DeckCardDTO("Sol Ring",1)));
        doAnswer(invocation -> {
            Deck d = invocation.getArgument(0);
            d.setId(1L);
            return null;
        }).when(deckRepository).persist(any(Deck.class));

        DeckResponseDTO resp = deckService.createDeck(request);

        assertNotNull(resp.id());
        assertEquals("My Deck", resp.name());
    }

    @Test
    void createDeck_validationFails() {
        DeckRequestDTO request = new DeckRequestDTO(" ", "Commander", List.of());
        assertThrows(IllegalArgumentException.class, () -> deckService.createDeck(request));
    }

    @Test
    void updateDeck_notFound() {
        Long id = 999L;
        when(deckRepository.findById(id)).thenReturn(null);
        DeckRequestDTO request = new DeckRequestDTO("Name","Cmd", List.of(new DeckCardDTO("Sol Ring",1)));
        assertNull(deckService.updateDeck(id, request));
    }
}
