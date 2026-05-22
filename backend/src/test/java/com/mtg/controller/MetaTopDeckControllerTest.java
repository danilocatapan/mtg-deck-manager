package com.mtg.controller;

import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.CardLookupRequestDTO;
import com.mtg.model.Deck;
import com.mtg.repository.DeckRepository;
import com.mtg.repository.MetaTopDeckImportBatchRepository;
import com.mtg.repository.MetaTopDeckRepository;
import com.mtg.service.CardService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.SecurityAttribute;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@QuarkusTest
@TestProfile(MetaTopDeckControllerTest.Profile.class)
class MetaTopDeckControllerTest {

    @InjectMock
    CardService cardService;

    @Inject
    MetaTopDeckRepository topDeckRepository;

    @Inject
    MetaTopDeckImportBatchRepository batchRepository;

    @Inject
    DeckRepository deckRepository;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void setup() {
        lenient().when(cardService.normalizeLookupName(anyString())).thenAnswer(invocation -> normalize(invocation.getArgument(0)));
        lenient().when(cardService.findByNames(any())).thenAnswer(invocation -> resolvedCards(invocation.getArgument(0)));
        lenient().when(cardService.findByCardRequests(any())).thenAnswer(invocation -> resolvedLookupCards(invocation.getArgument(0)));
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createQuery("delete from MetaTopDeckCard").executeUpdate();
            topDeckRepository.deleteAll();
            batchRepository.deleteAll();
            entityManager.createQuery("delete from DeckCard card where card.deck.ownerId = :owner")
                    .setParameter("owner", "external-import")
                    .executeUpdate();
            deckRepository.delete("ownerId", "external-import");
        });
    }

    @Test
    void importsTopDecksRefreshesProfilesAndCreatesPublicDeckProjection() {
        given()
                .header("X-Admin-Key", "top-deck-test-key")
                .contentType(ContentType.JSON)
                .body(topDeckPayload("https://moxfield.test/talion-1", 1, "Talion Top 1", "Thassa's Oracle", "Demonic Consultation"))
                .when().post("/meta/top-decks/import")
                .then()
                .statusCode(200)
                .body("status", is("SUCCESS"))
                .body("createdDecks", is(1))
                .body("updatedDecks", is(0))
                .body("importedDecks", is(1))
                .body("warnings", hasItem(containsString("expected 100")));

        given()
                .header("X-Admin-Key", "top-deck-test-key")
                .queryParam("commander", "Talion")
                .when().get("/meta/top-decks")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].commander", is("Talion, the Kindly Lord"))
                .body("[0].bracket", is("BRACKET_5"))
                .body("[0].cardsCount", is(3));

        Deck publicDeck = deckRepository.find("externalDeckUrl", "https://moxfield.test/talion-1").firstResult();
        org.junit.jupiter.api.Assertions.assertNotNull(publicDeck);
        org.junit.jupiter.api.Assertions.assertEquals("external", publicDeck.getSourceType());
        org.junit.jupiter.api.Assertions.assertEquals("MOXFIELD", publicDeck.getExternalSource());
        org.junit.jupiter.api.Assertions.assertEquals(2, publicDeck.getCards().size());

        given()
                .queryParam("commander", "Talion")
                .when().get("/public/decks")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].sourceType", is("external"));
    }

    @Test
    void reimportingSameSnapshotUpdatesInsteadOfDuplicating() {
        importDeck("https://moxfield.test/talion-1", 1, "Talion Top 1", "Thassa's Oracle");
        importDeck("https://moxfield.test/talion-1", 1, "Talion Top 1 Updated", "Mystic Remora");

        org.junit.jupiter.api.Assertions.assertEquals(1, topDeckRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(1, deckRepository.count("externalDeckUrl", "https://moxfield.test/talion-1"));
        given()
                .header("X-Admin-Key", "top-deck-test-key")
                .when().get("/meta/top-decks/" + topDeckRepository.listAll().getFirst().getId())
                .then()
                .statusCode(200)
                .body("name", is("Talion Top 1 Updated"))
                .body("cards.name", hasItem("Mystic Remora"));
    }

    @Test
    void importsTextTopDeckPreservingPrintingMetadata() {
        given()
                .header("X-Admin-Key", "top-deck-test-key")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "source": "MOXFIELD",
                          "sourceUrl": "https://moxfield.com",
                          "rankingPeriod": "MONTHLY",
                          "rankingDate": "2026-05-01",
                          "format": "COMMANDER",
                          "importFormat": "TEXT",
                          "decklistFormat": "MOXFIELD",
                          "decks": [
                            {
                              "rank": 1,
                              "name": "Text Talion",
                              "commander": "Talion, the Kindly Lord",
                              "deckUrl": "https://moxfield.test/text-talion",
                              "bracket": "BRACKET_5",
                              "decklist": "1 Talion, the Kindly Lord\\n1 Winota, Joiner of Forces (IKO) 349 *F*"
                            }
                          ]
                        }
                        """)
                .when().post("/meta/top-decks/import")
                .then()
                .statusCode(200)
                .body("importedDecks", is(1));

        Long deckId = topDeckRepository.listAll().getFirst().getId();
        given()
                .header("X-Admin-Key", "top-deck-test-key")
                .when().get("/meta/top-decks/" + deckId)
                .then()
                .statusCode(200)
                .body("cards.find { it.name == 'Winota, Joiner of Forces' }.setCode", is("IKO"))
                .body("cards.find { it.name == 'Winota, Joiner of Forces' }.collectorNumber", is("349"))
                .body("cards.find { it.name == 'Winota, Joiner of Forces' }.finish", is("FOIL"));
    }

    @Test
    void sameDeckInAnotherMonthCreatesHistoricalSnapshot() {
        importDeck("https://moxfield.test/talion-1", 1, "Talion May", "Thassa's Oracle", "2026-05-01");
        importDeck("https://moxfield.test/talion-1", 1, "Talion June", "Thassa's Oracle", "2026-06-01");

        org.junit.jupiter.api.Assertions.assertEquals(2, topDeckRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(2, deckRepository.count("externalDeckUrl", "https://moxfield.test/talion-1"));
    }

    @Test
    void rejectsInvalidSectionsAndCommanderResolutionFailuresPerDeck() {
        given()
                .header("X-Admin-Key", "top-deck-test-key")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "source": "MOXFIELD",
                          "sourceUrl": "https://moxfield.com",
                          "rankingPeriod": "MONTHLY",
                          "rankingDate": "2026-05-01",
                          "format": "COMMANDER",
                          "decks": [
                            {
                              "rank": 1,
                              "name": "Invalid Section",
                              "commander": "Talion, the Kindly Lord",
                              "bracket": "BRACKET_5",
                              "cards": [
                                { "name": "Talion, the Kindly Lord", "quantity": 1, "section": "COMMANDER" },
                                { "name": "Sol Ring", "quantity": 1, "section": "MAYBEBOARD" }
                              ]
                            },
                            {
                              "rank": 2,
                              "name": "Missing Commander",
                              "commander": "Missing Commander",
                              "bracket": "BRACKET_5",
                              "cards": [
                                { "name": "Missing Commander", "quantity": 1, "section": "COMMANDER" },
                                { "name": "Sol Ring", "quantity": 1, "section": "MAIN" }
                              ]
                            }
                          ]
                        }
                        """)
                .when().post("/meta/top-decks/import")
                .then()
                .statusCode(200)
                .body("status", is("FAILED"))
                .body("ignoredDecks", is(2))
                .body("warningsCount", is(3));
    }

    @Test
    void syncEndpointRegistersBatchAndRequiresAdminKey() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"source\":\"MANUAL\",\"rankingPeriod\":\"MONTHLY\",\"rankingDate\":\"2026-05-01\"}")
                .when().post("/meta/top-decks/sync")
                .then()
                .statusCode(403);

        given()
                .header("X-Admin-Key", "top-deck-test-key")
                .contentType(ContentType.JSON)
                .body("{\"source\":\"MANUAL\",\"rankingPeriod\":\"MONTHLY\",\"rankingDate\":\"2026-05-01\"}")
                .when().post("/meta/top-decks/sync")
                .then()
                .statusCode(200)
                .body("status", is("SUCCESS"));
    }

    @Test
    @TestSecurity(user = "meta-admin-user", attributes = {
            @SecurityAttribute(key = "email", value = "dcatapan@gmail.com")
    })
    void topDeckEndpointsAllowConfiguredGoogleAdminEmail() {
        given()
                .contentType(ContentType.JSON)
                .body(topDeckPayload("https://moxfield.test/google-admin", 1, "Talion Google Admin", "Mystic Remora", "Demonic Consultation"))
                .when().post("/meta/top-decks/import")
                .then()
                .statusCode(200)
                .body("status", is("SUCCESS"));

        given()
                .queryParam("commander", "Talion")
                .when().get("/meta/top-decks")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    @TestSecurity(user = "regular-google-user", attributes = {
            @SecurityAttribute(key = "email", value = "outro@example.com")
    })
    void topDeckEndpointsRejectNonAdminGoogleEmail() {
        given()
                .when().get("/meta/top-decks")
                .then()
                .statusCode(403);
    }

    private void importDeck(String deckUrl, int rank, String name, String card) {
        importDeck(deckUrl, rank, name, card, "2026-05-01");
    }

    private void importDeck(String deckUrl, int rank, String name, String card, String rankingDate) {
        given()
                .header("X-Admin-Key", "top-deck-test-key")
                .contentType(ContentType.JSON)
                .body(topDeckPayload(deckUrl, rank, name, card, "Demonic Consultation", rankingDate))
                .when().post("/meta/top-decks/import")
                .then()
                .statusCode(200);
    }

    private String topDeckPayload(String deckUrl, int rank, String name, String firstCard, String secondCard) {
        return topDeckPayload(deckUrl, rank, name, firstCard, secondCard, "2026-05-01");
    }

    private String topDeckPayload(String deckUrl, int rank, String name, String firstCard, String secondCard, String rankingDate) {
        return """
                {
                  "source": "MOXFIELD",
                  "sourceUrl": "https://moxfield.com",
                  "rankingPeriod": "MONTHLY",
                  "rankingDate": "%s",
                  "format": "COMMANDER",
                  "decks": [
                    {
                      "rank": %d,
                      "name": "%s",
                      "commander": "Talion, the Kindly Lord",
                      "deckUrl": "%s",
                      "archetype": "CONTROL",
                      "bracket": "BRACKET_5",
                      "colorIdentity": ["U", "B"],
                      "wins": 5,
                      "losses": 1,
                      "popularityScore": 95.0,
                      "cards": [
                        { "name": "Talion, the Kindly Lord", "quantity": 1, "section": "COMMANDER" },
                        { "name": "%s", "quantity": 1, "section": "MAIN" },
                        { "name": "%s", "quantity": 1, "section": "MAIN" }
                      ]
                    }
                  ]
                }
                """.formatted(rankingDate, rank, name, deckUrl, firstCard, secondCard);
    }

    private Map<String, CardResponseDTO> resolvedCards(List<String> names) {
        Map<String, CardResponseDTO> cards = new LinkedHashMap<>();
        for (String name : names) {
            if (name == null || name.isBlank() || normalize(name).equals("missing commander")) {
                continue;
            }
            cards.put(normalize(name), cardFor(name));
        }
        return cards;
    }

    private Map<String, CardResponseDTO> resolvedLookupCards(List<CardLookupRequestDTO> lookups) {
        Map<String, CardResponseDTO> cards = new LinkedHashMap<>();
        for (CardLookupRequestDTO lookup : lookups) {
            if (lookup == null || lookup.name() == null || lookup.name().isBlank() || normalize(lookup.name()).equals("missing commander")) {
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
        if (normalized.equals("talion, the kindly lord")) {
            return new CardResponseDTO(name.trim(), "{2}{U}{B}", "Legendary Creature - Faerie Noble", "", 4.0, List.of("U", "B"), List.of());
        }
        if (normalized.equals("demonic consultation")) {
            return new CardResponseDTO(name.trim(), "{B}", "Instant", "", 1.0, List.of("B"), List.of());
        }
        return new CardResponseDTO(name.trim(), "{U}", "Instant", "", 1.0, List.of("U"), List.of());
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("meta.sync.api-key", "top-deck-test-key");
        }
    }
}
