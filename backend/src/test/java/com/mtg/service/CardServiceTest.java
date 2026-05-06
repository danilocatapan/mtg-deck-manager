package com.mtg.service;

import com.mtg.client.ScryfallClient;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.ScryfallCardDTO;
import com.mtg.dto.ScryfallCollectionRequestDTO;
import com.mtg.dto.ScryfallCollectionResponseDTO;
import com.mtg.dto.ScryfallResponseDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.MockitoConfig;
import java.util.List;
import java.util.Map;
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
                    1.0,
                    "Artifact",
                    "{T}: Add {C}{C}.",
                    java.util.List.of()
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
    void shouldDeduplicateCardsByName() {
        when(scryfallClient.searchByName("\"Mountain\"")).thenReturn(
                new ScryfallResponseDTO(List.of(
                        new ScryfallCardDTO(
                                "Mountain",
                                "",
                                0.0,
                                "Basic Land - Mountain",
                                "({T}: Add {R}.)",
                                java.util.List.of("R")
                        ),
                        new ScryfallCardDTO(
                                "Mountain",
                                "",
                                0.0,
                                "Basic Land - Mountain",
                                "({T}: Add {R}.)",
                                java.util.List.of("R")
                        )
                ))
        );

        List<CardResponseDTO> cards = cardService.searchByName("Mountain");

        assertEquals(1, cards.size());
        assertEquals("Mountain", cards.get(0).name());
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
                    2.0,
                    "Artifact",
                    "{T}: Add one mana of any color in your commander's color identity.",
                    java.util.List.of()
                )))
        );

        List<CardResponseDTO> firstResult = cardService.searchByName("Arcane Signet");
        List<CardResponseDTO> secondResult = cardService.searchByName("Arcane Signet");

        assertFalse(firstResult.isEmpty());
        assertEquals(firstResult, secondResult);
        verify(scryfallClient, times(1)).searchByName("\"Arcane Signet\"");
    }

    @Test
    void shouldFetchCardsByNamesUsingCollectionEndpoint() {
        when(scryfallClient.collection(new ScryfallCollectionRequestDTO(List.of(
                new ScryfallCollectionRequestDTO.CardIdentifier("Batch Stone"),
                new ScryfallCollectionRequestDTO.CardIdentifier("Batch Insight")
        )))).thenReturn(new ScryfallCollectionResponseDTO(
                List.of(
                        new ScryfallCardDTO(
                                "Batch Stone",
                                "{1}",
                                1.0,
                                "Artifact",
                                "{T}: Add {C}{C}.",
                                java.util.List.of()
                        ),
                        new ScryfallCardDTO(
                                "Batch Insight",
                                "{U}",
                                1.0,
                                "Instant",
                                "Draw a card.",
                                java.util.List.of("U")
                        )
                ),
                List.of()
        ));

        Map<String, CardResponseDTO> cards = cardService.findByNames(List.of("Batch Stone", "Batch Insight"));

        assertEquals(2, cards.size());
        assertEquals("Batch Stone", cards.get("batch stone").name());
        assertEquals("Draw a card.", cards.get("batch insight").oracleText());
        verify(scryfallClient, times(1)).collection(new ScryfallCollectionRequestDTO(List.of(
                new ScryfallCollectionRequestDTO.CardIdentifier("Batch Stone"),
                new ScryfallCollectionRequestDTO.CardIdentifier("Batch Insight")
        )));
    }

    @Test
    void shouldFallbackToIndividualSearchForCollectionMisses() {
        when(scryfallClient.collection(new ScryfallCollectionRequestDTO(List.of(
                new ScryfallCollectionRequestDTO.CardIdentifier("Fallback Card")
        )))).thenReturn(new ScryfallCollectionResponseDTO(
                List.of(),
                List.of(new ScryfallCollectionRequestDTO.CardIdentifier("Fallback Card"))
        ));
        when(scryfallClient.searchByName("\"Fallback Card\"")).thenReturn(
                new ScryfallResponseDTO(List.of(new ScryfallCardDTO(
                        "Fallback Card",
                        "{2}",
                        2.0,
                        "Artifact",
                        "Draw a card.",
                        java.util.List.of()
                )))
        );

        Map<String, CardResponseDTO> cards = cardService.findByNames(List.of("Fallback Card"));

        assertEquals(1, cards.size());
        assertEquals("Fallback Card", cards.get("fallback card").name());
        verify(scryfallClient, times(1)).searchByName("\"Fallback Card\"");
    }
}
