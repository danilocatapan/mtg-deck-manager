package com.mtg.controller;

import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.DeckRequestDTO;
import com.mtg.dto.ApplyRecommendationSwapDTO;
import com.mtg.dto.CardResponseDTO;
import com.mtg.service.CardService;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.junit.QuarkusTest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@QuarkusTest
class DeckControllerTest {

    @InjectMock
    CardService cardService;

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
}
