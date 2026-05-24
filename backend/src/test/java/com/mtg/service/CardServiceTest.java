package com.mtg.service;

import com.mtg.client.ScryfallClient;
import com.mtg.dto.CardLookupRequestDTO;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.ScryfallCardDTO;
import com.mtg.dto.ScryfallCollectionRequestDTO;
import com.mtg.dto.ScryfallCollectionResponseDTO;
import com.mtg.dto.ScryfallResponseDTO;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.MockitoConfig;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
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
    void shouldExposeUsdPriceEstimateWhenAvailable() {
        when(scryfallClient.searchByName("\"Nature's Lore\"")).thenReturn(
                new ScryfallResponseDTO(List.of(new ScryfallCardDTO(
                        "Nature's Lore",
                        "{1}{G}",
                        2.0,
                        "Sorcery",
                        "Search your library for a Forest card.",
                        java.util.List.of("G"),
                        null,
                        null,
                        new ScryfallCardDTO.PricesDTO("2.35", null, null, null)
                )))
        );

        List<CardResponseDTO> cards = cardService.searchByName("Nature's Lore");

        assertEquals(2.35, cards.getFirst().estimatedPrice());
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
    void shouldWrapRateLimitFailures() {
        when(scryfallClient.searchByName(anyString())).thenThrow(new WebApplicationException(
                Response.status(429).build()
        ));

        assertThrows(RateLimitedExternalServiceException.class, () -> cardService.searchByName("Rate Limited Card"));
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

    @Test
    void shouldResolveSpecificPrintingsWithoutIndividualFallback() {
        when(scryfallClient.collection(new ScryfallCollectionRequestDTO(List.of(
                ScryfallCollectionRequestDTO.CardIdentifier.printing("iko", "349")
        )))).thenReturn(new ScryfallCollectionResponseDTO(
                List.of(new ScryfallCardDTO(
                        "scryfall-winota",
                        "Winota, Joiner of Forces",
                        "iko",
                        "Ikoria: Lair of Behemoths",
                        "349",
                        "{2}{R}{W}",
                        4.0,
                        "Legendary Creature - Human Warrior",
                        "Whenever a non-Human creature you control attacks...",
                        java.util.List.of("R", "W"),
                        null,
                        null,
                        java.util.List.of("foil", "nonfoil"),
                        new ScryfallCardDTO.PricesDTO("2.35", "5.10", null, null)
                )),
                List.of()
        ));

        Map<String, CardResponseDTO> cards = cardService.findByCardRequests(List.of(
                new CardLookupRequestDTO("Winota, Joiner of Forces", "IKO", "349", null)
        ));

        CardResponseDTO card = cards.get(CardLookupRequestDTO.printingKey("IKO", "349"));
        assertEquals("Winota, Joiner of Forces", card.name());
        assertEquals("IKO", card.setCode());
        assertEquals("349", card.collectorNumber());
        assertEquals("scryfall-winota", card.scryfallId());
        verify(scryfallClient, times(1)).collection(new ScryfallCollectionRequestDTO(List.of(
                ScryfallCollectionRequestDTO.CardIdentifier.printing("iko", "349")
        )));
        verify(scryfallClient, times(0)).searchByName(anyString());
    }

    @Test
    void shouldMapModalDoubleFacedCardsUsingFrontFaceDetailsAndImage() {
        when(scryfallClient.collection(new ScryfallCollectionRequestDTO(List.of(
                new ScryfallCollectionRequestDTO.CardIdentifier("Agadeem's Awakening")
        )))).thenReturn(new ScryfallCollectionResponseDTO(
                List.of(new ScryfallCardDTO(
                        "scryfall-agadeem",
                        "Agadeem's Awakening // Agadeem, the Undercrypt",
                        "znr",
                        "Zendikar Rising",
                        "90",
                        null,
                        3.0,
                        "Sorcery // Land",
                        null,
                        java.util.List.of("B"),
                        null,
                        List.of(
                                new ScryfallCardDTO.CardFaceDTO(
                                        "Agadeem's Awakening",
                                        "{X}{B}{B}{B}",
                                        "Sorcery",
                                        "Return from your graveyard to the battlefield any number of target creature cards.",
                                        new ScryfallCardDTO.ImageUris("small-front.jpg", "normal-front.jpg", "large-front.jpg", "png-front.jpg")
                                ),
                                new ScryfallCardDTO.CardFaceDTO(
                                        "Agadeem, the Undercrypt",
                                        "",
                                        "Land",
                                        "As Agadeem, the Undercrypt enters the battlefield, you may pay 3 life.",
                                        new ScryfallCardDTO.ImageUris("small-back.jpg", "normal-back.jpg", "large-back.jpg", "png-back.jpg")
                                )
                        ),
                        java.util.List.of("nonfoil", "foil"),
                        new ScryfallCardDTO.PricesDTO("12.34", null, null, null)
                )),
                List.of()
        ));

        Map<String, CardResponseDTO> cards = cardService.findByNames(List.of("Agadeem's Awakening"));

        CardResponseDTO card = cards.get("agadeem's awakening");
        assertEquals("Agadeem's Awakening", card.name());
        assertEquals("{X}{B}{B}{B}", card.manaCost());
        assertEquals("Sorcery", card.typeLine());
        assertEquals("Return from your graveyard to the battlefield any number of target creature cards.", card.oracleText());
        assertEquals("normal-front.jpg", card.imageUrl());
        assertEquals("ZNR", card.setCode());
        assertEquals("90", card.collectorNumber());
    }
}
