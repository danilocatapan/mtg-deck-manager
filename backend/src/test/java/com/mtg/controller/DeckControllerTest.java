package com.mtg.controller;

import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.DeckRequestDTO;
import com.mtg.dto.ApplyRecommendationSwapDTO;
import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.model.DeckVisibility;
import com.mtg.repository.DeckRepository;
import com.mtg.service.CardService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.SecurityAttribute;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
class DeckControllerTest {

    @InjectMock
    CardService cardService;

    @Inject
    DeckRepository deckRepository;

    @BeforeEach
    void setup() {
        lenient().when(cardService.normalizeLookupName(anyString())).thenAnswer(invocation -> normalize(invocation.getArgument(0)));
        lenient().when(cardService.findByNames(any())).thenAnswer(invocation -> resolvedCards(invocation.getArgument(0)));
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void createAndGetDeck() {
        DeckRequestDTO request = new DeckRequestDTO("MyDeck","Cmd", List.of(new DeckCardDTO("Sol Ring",1)));

        Response r = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/decks")
                .then().statusCode(201)
                .extract().response();

        String location = r.getHeader("Location");
        Response get = given().when().get(location).then().statusCode(200).extract().response();
        assertThat(get.jsonPath().getString("name"), is("MyDeck"));
        assertThat(get.jsonPath().getString("colorIdentity"), is("G"));
        assertThat(get.jsonPath().getString("commanders[0].name"), is("Cmd"));
        assertThat(get.jsonPath().getString("visibility"), is("private"));
    }

    @Test
    @TestSecurity(user = "google-user-1", attributes = {
            @SecurityAttribute(key = "name", value = "Public Brewer")
    })
    void createUpdateListAndConsultVisibility() {
        Map<String, Object> publicRequest = Map.of(
                "name", "Public Visibility Deck",
                "commander", "Cmd",
                "visibility", "public",
                "cards", List.of(Map.of("name", "Sol Ring", "quantity", 1))
        );

        Response created = given()
                .contentType(ContentType.JSON)
                .body(publicRequest)
                .when().post("/decks")
                .then()
                .statusCode(201)
                .body("visibility", is("public"))
                .extract().response();

        String location = created.getHeader("Location");

        given()
                .when().get("/decks/public")
                .then()
                .statusCode(200)
                .body("name", hasItem("Public Visibility Deck"))
                .body("visibility", hasItem("public"))
                .body("author", hasItem("Public Brewer"));

        given()
                .when().get(location + "/consult")
                .then()
                .statusCode(200)
                .body("name", is("Public Visibility Deck"))
                .body("visibility", is("public"))
                .body("author", is("Public Brewer"))
                .body("$", not(org.hamcrest.Matchers.hasKey("history")))
                .body("$", not(org.hamcrest.Matchers.hasKey("ownerId")));

        Map<String, Object> privateUpdate = Map.of(
                "name", "Private Visibility Deck",
                "commander", "Cmd",
                "visibility", "private",
                "cards", List.of(Map.of("name", "Sol Ring", "quantity", 1))
        );

        given()
                .contentType(ContentType.JSON)
                .body(privateUpdate)
                .when().put(location)
                .then()
                .statusCode(200)
                .body("visibility", is("private"));

        given()
                .when().get("/decks/public")
                .then()
                .statusCode(200)
                .body("name", not(hasItem("Private Visibility Deck")));
    }

    @Test
    @TestSecurity(user = "legacy-owner", attributes = {
            @SecurityAttribute(key = "name", value = "Legacy Brewer")
    })
    void updateVisibilityForLegacyDeck_doesNotRevalidateUnchangedCards() {
        Long deckId = persistDeck("Legacy Visibility Deck", DeckVisibility.PRIVATE, "legacy-owner", null, "Missing Card");
        clearInvocations(cardService);

        Map<String, Object> publicUpdate = Map.of(
                "name", "Legacy Visibility Deck",
                "commander", "Cmd",
                "visibility", "public",
                "cards", List.of(Map.of("name", "Missing Card", "quantity", 1))
        );

        given()
                .contentType(ContentType.JSON)
                .body(publicUpdate)
                .when().put("/decks/" + deckId)
                .then()
                .statusCode(200)
                .body("visibility", is("public"));

        verify(cardService, never()).findByNames(any());

        given()
                .when().get("/decks/public")
                .then()
                .statusCode(200)
                .body("name", hasItem("Legacy Visibility Deck"))
                .body("author", hasItem("Legacy Brewer"));
    }

    @Test
    void consultPublicDeck_allowsAnonymous() {
        Long deckId = persistDeck("Anonymous Public Consult", DeckVisibility.PUBLIC, "owner-public", "Public Author");

        given()
                .when().get("/decks/" + deckId + "/consult")
                .then()
                .statusCode(200)
                .body("name", is("Anonymous Public Consult"))
                .body("visibility", is("public"))
                .body("author", is("Public Author"));
    }

    @Test
    void consultPrivateDeck_rejectsAnonymous() {
        Long deckId = persistDeck("Anonymous Private Consult", DeckVisibility.PRIVATE, "owner-private", "Private Author");

        given()
                .when().get("/decks/" + deckId + "/consult")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "private-owner")
    void consultPrivateDeck_allowsOwner() {
        Long deckId = persistDeck("Owner Private Consult", DeckVisibility.PRIVATE, "private-owner", "Private Author");

        given()
                .when().get("/decks/" + deckId + "/consult")
                .then()
                .statusCode(200)
                .body("name", is("Owner Private Consult"))
                .body("visibility", is("private"));
    }

    @Test
    @TestSecurity(user = "other-user")
    void consultPrivateDeck_rejectsOtherUsers() {
        Long deckId = persistDeck("Other Private Consult", DeckVisibility.PRIVATE, "private-owner-2", "Private Author");

        given()
                .when().get("/decks/" + deckId + "/consult")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void createDeck_returnsBadRequestForInvalidVisibility() {
        Map<String, Object> request = Map.of(
                "name", "Invalid Visibility Deck",
                "commander", "Cmd",
                "visibility", "friends-only",
                "cards", List.of(Map.of("name", "Sol Ring", "quantity", 1))
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/decks")
                .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void createDeck_acceptsMultipleCommanders() {
        Map<String, Object> request = Map.of(
                "name", "Partner Deck",
                "commander", "Wilson, Refined Grizzly",
                "commanders", List.of(
                        Map.of("name", "Wilson, Refined Grizzly", "role", "commander"),
                        Map.of("name", "Raised by Giants", "role", "background")
                ),
                "cards", List.of(Map.of("name", "Sol Ring", "quantity", 1))
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/decks")
                .then()
                .statusCode(201)
                .body("commander", is("Wilson, Refined Grizzly"))
                .body("colorIdentity", is("G"))
                .body("commanders.size()", is(2))
                .body("commanders.name", org.hamcrest.Matchers.hasItems("Wilson, Refined Grizzly", "Raised by Giants"));
    }

    @Test
    void createDeck_requiresAuthentication() {
        DeckRequestDTO request = new DeckRequestDTO("MyDeck","Cmd", List.of(new DeckCardDTO("Sol Ring",1)));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/decks")
                .then().statusCode(401);
    }

    @Test
    void importDeck_preflightAllowsGithubPagesOrigin() {
        given()
                .header("Origin", "https://danilocatapan.github.io")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .when().options("/decks/import")
                .then()
                .statusCode(anyOf(is(200), is(204)))
                .header("Access-Control-Allow-Origin", "https://danilocatapan.github.io");
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void getNotFound() {
        given().when().get("/decks/999999").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void exportDeck_returnsTextPlain() {
        DeckRequestDTO request = new DeckRequestDTO("MyDeck","Cmd", List.of(new DeckCardDTO("Sol Ring",1)));

        Response r = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/decks")
                .then().statusCode(201)
                .extract().response();

        String location = r.getHeader("Location");
        Response exp = given().when().get(location + "/export").then().statusCode(200).extract().response();
        assertThat(exp.getHeader("Content-Type"), is("text/plain;charset=UTF-8"));
        assertThat(exp.asString().contains("Sol Ring"), is(true));
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void exportDeck_notFound() {
        given().when().get("/decks/999999/export").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void legality_returnsCommanderContractReport() {
        DeckRequestDTO request = new DeckRequestDTO("Legal Deck", "Cmd", List.of(
                new DeckCardDTO("Forest", 98),
                new DeckCardDTO("Sol Ring", 1)
        ));

        Response created = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/decks")
                .then().statusCode(201)
                .extract().response();

        given()
                .when().get(created.getHeader("Location") + "/legality")
                .then()
                .statusCode(200)
                .body("mainDeckSize", is(99))
                .body("sizeLegal", is(true))
                .body("singletonLegal", is(true))
                .body("colorIdentityLegal", is(true))
                .body("banlist.legal", is(true))
                .body("commanderValid", is(true))
                .body("estimatedBracket.level", is(2))
                .body("gameChangerCount", is(0))
                .body("rulesSnapshot.banlistDate", is("2026-05-07"))
                .body("rulesSnapshot.gameChangersDate", is("2026-02-09"))
                .body("legal", is(true));
    }

    @Test
    void legality_requiresAuthentication() {
        given().when().get("/decks/1/legality").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void applyRecommendationSwap_updatesDeck() {
        DeckRequestDTO request = new DeckRequestDTO("MyDeck", "Cmd", List.of(
                new DeckCardDTO("Sol Ring", 1),
                new DeckCardDTO("Naturalize", 1)
        ));

        Response created = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/decks")
                .then().statusCode(201)
                .extract().response();

        given()
                .contentType(ContentType.JSON)
                .body(new ApplyRecommendationSwapDTO("Beast Within", "Naturalize"))
                .when().post(created.getHeader("Location") + "/recommendations/apply-swap")
                .then()
                .statusCode(200)
                .body("cards.name", org.hamcrest.Matchers.hasItem("Beast Within"))
                .body("cards.name", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Naturalize")));
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void applyRecommendationSwap_returnsBadRequestForInvalidSwap() {
        DeckRequestDTO request = new DeckRequestDTO("MyDeck", "Cmd", List.of(
                new DeckCardDTO("Sol Ring", 1),
                new DeckCardDTO("Naturalize", 1)
        ));

        Response created = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/decks")
                .then().statusCode(201)
                .extract().response();

        given()
                .contentType(ContentType.JSON)
                .body(new ApplyRecommendationSwapDTO("Sol Ring", "Naturalize"))
                .when().post(created.getHeader("Location") + "/recommendations/apply-swap")
                .then()
                .statusCode(400)
                .body("message", is("Card to add already exists in deck"));
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void applyRecommendationSwap_returnsNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body(new ApplyRecommendationSwapDTO("Beast Within", "Naturalize"))
                .when().post("/decks/999999/recommendations/apply-swap")
                .then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void createDeck_returnsBadRequestWhenCardDoesNotExist() {
        DeckRequestDTO request = new DeckRequestDTO("MyDeck", "Cmd", List.of(new DeckCardDTO("Missing Card", 1)));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/decks")
                .then()
                .statusCode(400)
                .body("message", is("Card not found: Missing Card"));
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

    private CardResponseDTO cardFor(String name) {
        String normalized = normalize(name);
        return switch (normalized) {
            case "cmd" -> new CardResponseDTO(name.trim(), "{2}{G}", "Legendary Creature - Elf", "", 3.0, List.of("G"), List.of());
            case "wilson, refined grizzly" -> new CardResponseDTO(name.trim(), "{1}{G}", "Legendary Creature - Bear Warrior", "Choose a Background.", 2.0, List.of("G"), List.of());
            case "raised by giants" -> new CardResponseDTO(name.trim(), "{5}{G}", "Legendary Enchantment - Background", "Commander creatures you own have base power and toughness 10/10.", 6.0, List.of("G"), List.of());
            case "forest" -> new CardResponseDTO(name.trim(), "", "Basic Land - Forest", "Add {G}.", 0.0, List.of("G"), List.of());
            case "sol ring" -> new CardResponseDTO(name.trim(), "{1}", "Artifact", "{T}: Add {C}{C}.", 1.0, List.of(), List.of());
            default -> new CardResponseDTO(name.trim(), "", "", "", 0.0, List.of(), List.of());
        };
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private Long persistDeck(String name, DeckVisibility visibility, String ownerId, String author) {
        return persistDeck(name, visibility, ownerId, author, "Sol Ring");
    }

    private Long persistDeck(String name, DeckVisibility visibility, String ownerId, String author, String cardName) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Deck deck = new Deck(name, "Cmd", List.of(new DeckCard(cardName, 1)));
            deck.setOwnerId(ownerId);
            deck.setAuthorDisplayName(author);
            deck.setVisibility(visibility);
            deck.setColorIdentity("G");
            deckRepository.persist(deck);
            return deck.getId();
        });
    }
}
