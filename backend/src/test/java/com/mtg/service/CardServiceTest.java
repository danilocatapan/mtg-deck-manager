package com.mtg.service;

import com.mtg.client.ScryfallClient;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.ScryfallCardResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    ScryfallClient scryfallClient;

    @InjectMocks
    CardService cardService;

    @BeforeEach
    void setUp() {
        // InjectMocks supports constructor injection automatically
    }

    @Test
    void shouldTransformScryfallResponseIntoInternalDto() {
        when(scryfallClient.findByName("Sol Ring")).thenReturn(
                new ScryfallCardResponseDTO("Sol Ring", "{0}", 0.0, "Artifact")
        );

        CardResponseDTO card = cardService.findByName("Sol Ring");

        assertEquals("Sol Ring", card.name());
        assertEquals(0.0, card.cmc());
        assertEquals("{0}", card.manaCost());
        assertEquals("Artifact", card.type());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> cardService.findByName("  "));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenScryfallReturnsNull() {
        when(scryfallClient.findByName(anyString())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> cardService.findByName("Sol Ring"));
    }
}
