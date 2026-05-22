package com.mtg.controller;

import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.CardLookupRequestDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.model.DeckVisibility;
import com.mtg.repository.DeckRepository;
import com.mtg.service.CardService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@QuarkusTest
@TestProfile(MetaExternalDeckImportTest.Profile.class)
class MetaExternalDeckImportTest {

    @InjectMock
    CardService cardService;

    @Inject
    DeckRepository deckRepository;

    @BeforeEach
    void setup() {
        lenient().when(cardService.normalizeLookupName(anyString())).thenAnswer(invocation -> normalize(invocation.getArgument(0)));
        lenient().when(cardService.findByNames(any())).thenAnswer(invocation -> resolvedCards(invocation.getArgument(0)));
        lenient().when(cardService.findByCardRequests(any())).thenAnswer(invocation -> resolvedLookupCards(invocation.getArgument(0)));
    }

    @Test
    void importStructuredDeckCreatesExternalPublicDeck() {
        given()
                .header("X-Admin-Key", "import-test-key")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "source": "LIGAMAGIC",
                          "sourceUrl": "https://www.ligamagic.com.br/",
                          "format": "COMMANDER",
                          "importFormat": "STRUCTURED",
                          "decks": [
                            {
                              "rank": 7,
                              "name": "Imported Atraxa",
                              "commander": "Atraxa, Praetors' Voice",
                              "deckUrl": "https://www.ligamagic.com.br/deck",
                              "cards": [
                                { "name": "Sol Ring", "quantity": 1, "section": "MAIN" },
                                { "name": "Command Tower", "quantity": 1, "section": "MAIN" }
                              ]
                            }
                          ]
                        }
                        """)
                .when().post("/meta/external-decks/import")
                .then()
                .statusCode(200)
                .body("source", is("LIGAMAGIC"))
                .body("importedDecks", is(1))
                .body("ignoredDecks", is(0))
                .body("importedCards", is(2))
                .body("warnings[0]", containsString("expected 99"));

        Deck deck = deckRepository.find("name", "Imported Atraxa").firstResult();
        org.junit.jupiter.api.Assertions.assertNotNull(deck);
        org.junit.jupiter.api.Assertions.assertEquals(DeckVisibility.PUBLIC, deck.getVisibility());
        org.junit.jupiter.api.Assertions.assertEquals("external-import", deck.getOwnerId());
        org.junit.jupiter.api.Assertions.assertNull(deck.getAuthorDisplayName());
        org.junit.jupiter.api.Assertions.assertEquals("external", deck.getSourceType());
        org.junit.jupiter.api.Assertions.assertEquals("LIGAMAGIC", deck.getExternalSource());
        org.junit.jupiter.api.Assertions.assertEquals(7, deck.getExternalRank());
        org.junit.jupiter.api.Assertions.assertEquals(2, deck.getCards().size());
    }

    @Test
    void importTextFormatsCreateExternalDecks() {
        for (String decklistFormat : List.of("MTG_ARENA", "LIGAMAGIC", "GENERIC")) {
            given()
                    .header("X-Admin-Key", "import-test-key")
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                              "source": "%s",
                              "format": "COMMANDER",
                              "importFormat": "TEXT",
                              "decklistFormat": "%s",
                              "decks": [
                                {
                                  "name": "Text Import %s",
                                  "deckUrl": "https://example.test/deck",
                                  "decklist": "Commander\\n1 Atraxa, Praetors' Voice\\n\\nDeck\\n1 Sol Ring (LTC) [foil]\\n1 Command Tower"
                                }
                              ]
                            }
                            """.formatted(decklistFormat, decklistFormat, decklistFormat))
                    .when().post("/meta/external-decks/import")
                    .then()
                    .statusCode(200)
                    .body("decklistFormat", is(decklistFormat))
                    .body("importedDecks", is(1))
                    .body("importedCards", is(2));
        }
    }

    @Test
    void importRequiresAdminKey() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "source": "LIGAMAGIC", "decks": [] }
                        """)
                .when().post("/meta/external-decks/import")
                .then()
                .statusCode(403);
    }

    @Test
    void importValidationErrorsReturnBadRequest() {
        given()
                .header("X-Admin-Key", "import-test-key")
                .contentType(ContentType.JSON)
                .body("""
                        { "format": "COMMANDER", "decks": [] }
                        """)
                .when().post("/meta/external-decks/import")
                .then()
                .statusCode(400)
                .body("message", is("source is required"));
    }

    @Test
    void unresolvedCardCreatesWarningAndDoesNotAlterExistingUserDeck() {
        Long existingDeckId = persistUserDeck();

        given()
                .header("X-Admin-Key", "import-test-key")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "source": "MOXFIELD",
                          "format": "COMMANDER",
                          "importFormat": "STRUCTURED",
                          "decks": [
                            {
                              "name": "Missing Card Import",
                              "commander": "Atraxa, Praetors' Voice",
                              "cards": [
                                { "name": "Sol Ring", "quantity": 1, "section": "MAIN" },
                                { "name": "Missing Card", "quantity": 1, "section": "MAIN" }
                              ]
                            }
                          ]
                        }
                        """)
                .when().post("/meta/external-decks/import")
                .then()
                .statusCode(200)
                .body("importedDecks", is(1))
                .body("importedCards", is(1))
                .body("warnings", hasItem(containsString("skipped unresolved card: Missing Card")));

        Deck existing = deckRepository.findById(existingDeckId);
        org.junit.jupiter.api.Assertions.assertEquals("Existing User Deck", existing.getName());
        org.junit.jupiter.api.Assertions.assertEquals("real-user", existing.getOwnerId());
        org.junit.jupiter.api.Assertions.assertEquals(DeckVisibility.PRIVATE, existing.getVisibility());
        org.junit.jupiter.api.Assertions.assertEquals(1, existing.getCards().size());
    }

    @Test
    @TestSecurity(user = "external-like-user")
    void publicImportedDeckCanReceiveLikes() {
        given()
                .header("X-Admin-Key", "import-test-key")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "source": "TOPDECK",
                          "format": "COMMANDER",
                          "importFormat": "STRUCTURED",
                          "decks": [
                            {
                              "name": "Imported Like Deck",
                              "commander": "Atraxa, Praetors' Voice",
                              "cards": [
                                { "name": "Sol Ring", "quantity": 1, "section": "MAIN" }
                              ]
                            }
                          ]
                        }
                        """)
                .when().post("/meta/external-decks/import")
                .then()
                .statusCode(200)
                .body("importedDecks", is(1));

        Deck imported = deckRepository.find("name", "Imported Like Deck").firstResult();

        given().when().post("/public/decks/" + imported.getId() + "/like")
                .then()
                .statusCode(200)
                .body("likeCount", is(1))
                .body("likedByCurrentUser", is(true));

        given()
                .when().get("/public/decks/" + imported.getId())
                .then()
                .statusCode(200)
                .body("sourceType", is("external"))
                .body("externalSource", is("TOPDECK"))
                .body("likeCount", is(1))
                .body("likedByCurrentUser", is(true))
                .body("id", notNullValue());
    }

    private Long persistUserDeck() {
        return QuarkusTransaction.requiringNew().call(() -> {
            Deck deck = new Deck("Existing User Deck", "Cmd", List.of(new DeckCard("Sol Ring", 1)));
            deck.setOwnerId("real-user");
            deck.setVisibility(DeckVisibility.PRIVATE);
            deck.setColorIdentity("G");
            deckRepository.persist(deck);
            return deck.getId();
        });
    }

    private Map<String, CardResponseDTO> resolvedCards(List<String> names) {
        Map<String, CardResponseDTO> cards = new LinkedHashMap<>();
        for (String name : names) {
            if (name == null || name.isBlank() || normalize(name).equals("missing card")) {
                continue;
            }
            cards.put(normalize(name), cardFor(name));
        }
        return cards;
    }

    private Map<String, CardResponseDTO> resolvedLookupCards(List<CardLookupRequestDTO> lookups) {
        Map<String, CardResponseDTO> cards = new LinkedHashMap<>();
        for (CardLookupRequestDTO lookup : lookups) {
            if (lookup == null || lookup.name() == null || lookup.name().isBlank() || normalize(lookup.name()).equals("missing card")) {
                continue;
            }
            CardResponseDTO base = cardFor(lookup.name());
            cards.put(lookup.lookupKey(), new CardResponseDTO(
                    base.name(),
                    base.manaCost(),
                    base.typeLine(),
                    base.oracleText(),
                    base.cmc(),
                    base.colorIdentity(),
                    base.keywords(),
                    "https://img.test/" + normalize(base.name()) + ".jpg",
                    null,
                    "scryfall-" + normalize(base.name()),
                    lookup.setCode(),
                    lookup.setCode() == null ? null : "Test Set",
                    lookup.collectorNumber(),
                    List.of("nonfoil")
            ));
        }
        return cards;
    }

    private CardResponseDTO cardFor(String name) {
        String normalized = normalize(name);
        if (normalized.equals("atraxa, praetors' voice")) {
            return new CardResponseDTO(name.trim(), "{G}{W}{U}{B}", "Legendary Creature - Phyrexian Angel Horror", "", 4.0, List.of("W", "U", "B", "G"), List.of());
        }
        if (normalized.equals("cmd")) {
            return new CardResponseDTO(name.trim(), "{2}{G}", "Legendary Creature - Elf", "", 3.0, List.of("G"), List.of());
        }
        return new CardResponseDTO(name.trim(), "", "Artifact", "", 0.0, List.of(), List.of());
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("meta.sync.api-key", "import-test-key");
        }
    }
}
