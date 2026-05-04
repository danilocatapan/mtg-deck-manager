package com.mtg.service;

import com.mtg.client.ScryfallClient;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.ScryfallCardDTO;
import com.mtg.dto.ScryfallResponseDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.MockitoConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class CardServiceTest {

    @InjectMock
    @MockitoConfig(convertScopes = true)
    @RestClient
    ScryfallClient scryfallClient;

    @jakarta.inject.Inject
    CardService cardService;

    @Test
    void shouldTransformScryfallResponseIntoInternalDto() {
        when(scryfallClient.searchByName("\"Sol Ring\"")).thenReturn(
                new ScryfallResponseDTO(List.of(new ScryfallCardDTO(
                        "Sol Ring",
                        "{1}",
                        "Artifact",
                        "{T}: Add {C}{C}."
                )))
        );

        List<CardResponseDTO> cards = cardService.searchByName("Sol Ring");

        assertEquals(1, cards.size());
        CardResponseDTO card = cards.get(0);
        assertEquals("Sol Ring", card.name());
        assertEquals("{1}", card.manaCost());
        assertEquals("Artifact", card.typeLine());
        assertEquals("{T}: Add {C}{C}.", card.oracleText());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> cardService.searchByName("  "));
    }

    @Test
    void shouldReturnEmptyListWhenScryfallDoesNotFindCards() {
        when(scryfallClient.searchByName(anyString())).thenThrow(new jakarta.ws.rs.NotFoundException());

        List<CardResponseDTO> cards = cardService.searchByName("Missing Card");

        assertTrue(cards.isEmpty());
    }

    @Test
    void shouldWrapExternalFailures() {
        when(scryfallClient.searchByName(anyString())).thenThrow(new jakarta.ws.rs.ProcessingException("timeout"));

        assertThrows(ExternalServiceException.class, () -> cardService.searchByName("Broken Card"));
    }

    @Test
    void shouldUseCacheForRepeatedSearches() {
        when(scryfallClient.searchByName("\"Arcane Signet\"")).thenReturn(
                new ScryfallResponseDTO(List.of(new ScryfallCardDTO(
                        "Arcane Signet",
                        "{2}",
                        "Artifact",
                        "{T}: Add one mana of any color in your commander's color identity."
                )))
        );

        List<CardResponseDTO> firstResult = cardService.searchByName("Arcane Signet");
        List<CardResponseDTO> secondResult = cardService.searchByName("Arcane Signet");

        assertFalse(firstResult.isEmpty());
        assertEquals(firstResult, secondResult);
        verify(scryfallClient, times(1)).searchByName("\"Arcane Signet\"");
    }
}
