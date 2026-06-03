package com.mtg.controller;

import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.DeckRequestDTO;
import com.mtg.model.RecommendationAuditRun;
import com.mtg.repository.RecommendationAuditRepository;
import com.mtg.service.CardService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.SecurityAttribute;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@QuarkusTest
class UserPrivacyControllerTest {

    @InjectMock
    CardService cardService;

    @Inject
    RecommendationAuditRepository auditRepository;

    @BeforeEach
    void setup() {
        lenient().when(cardService.normalizeLookupName(anyString())).thenAnswer(invocation -> normalize(invocation.getArgument(0)));
        lenient().when(cardService.findByNames(any())).thenAnswer(invocation -> resolvedCards(invocation.getArgument(0)));
    }

    @Test
    @TestSecurity(user = "privacy-export-user", attributes = {
            @SecurityAttribute(key = "email", value = "privacy@example.com"),
            @SecurityAttribute(key = "name", value = "Privacy User"),
            @SecurityAttribute(key = "picture", value = "https://example.com/avatar.png")
    })
    void exportData_returnsAuthenticatedUserDecksAndAudits() {
        Response created = createDeck("Privacy Export Deck");
        Long deckId = idFromLocation(created.getHeader("Location"));
        persistAudit(deckId, "privacy-export-user");

        given()
                .when().get("/users/me/export")
                .then()
                .statusCode(200)
                .body("user.googleSubject", is("privacy-export-user"))
                .body("user.email", is("privacy@example.com"))
                .body("user.name", is("Privacy User"))
                .body("user.avatarUrl", is("https://example.com/avatar.png"))
                .body("collectedData", hasItem("Google subject/id"))
                .body("decks.name", hasItem("Privacy Export Deck"))
                .body("recommendationAudits.deckId", hasItem(deckId.intValue()));
    }

    @Test
    @TestSecurity(user = "privacy-delete-user")
    void deleteAccount_removesDecksAndRecommendationAuditsForUser() {
        Response created = createDeck("Privacy Delete Deck");
        Long deckId = idFromLocation(created.getHeader("Location"));
        persistAudit(deckId, "privacy-delete-user");
        importCollection("2 Sol Ring\n1 Arcane Signet");

        given()
                .when().delete("/users/me")
                .then()
                .statusCode(204);

        given()
                .when().get("/users/me/export")
                .then()
                .statusCode(200)
                .body("decks", empty())
                .body("collection", empty())
                .body("recommendationAudits", empty());
    }

    @Test
    @TestSecurity(user = "collection-import-user")
    void collectionImportPersistsNormalizedQuantitiesAndAppearsInExport() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"content\":\"2 Sol Ring\\n1 Sol Ring\\n1 Arcane Signet\",\"replaceExisting\":true}")
                .when().post("/users/me/collection/import")
                .then()
                .statusCode(200)
                .body("importedCards", is(4))
                .body("uniqueCards", is(2))
                .body("warnings", hasItem("Cartas repetidas foram somadas na colecao importada."));

        given()
                .when().get("/users/me/collection")
                .then()
                .statusCode(200)
                .body("name", hasItem("Sol Ring"))
                .body("find { it.name == 'Sol Ring' }.quantity", is(3));

        given()
                .when().get("/users/me/export")
                .then()
                .statusCode(200)
                .body("collection.name", hasItem("Arcane Signet"));
    }

    @Test
    void exportData_requiresAuthentication() {
        given().when().get("/users/me/export").then().statusCode(401);
    }

    @Test
    void deleteAccount_requiresAuthentication() {
        given().when().delete("/users/me").then().statusCode(401);
    }

    @Test
    void collectionImportRequiresAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"content\":\"1 Sol Ring\"}")
                .when().post("/users/me/collection/import")
                .then().statusCode(401);
    }

    private Response createDeck(String name) {
        DeckRequestDTO request = new DeckRequestDTO(name, "Cmd", List.of(new DeckCardDTO("Sol Ring", 1)));
        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when().post("/decks")
                .then().statusCode(201)
                .extract().response();
    }

    void persistAudit(Long deckId, String ownerId) {
        QuarkusTransaction.requiringNew().run(() -> {
            RecommendationAuditRun audit = new RecommendationAuditRun();
            audit.setDeckId(deckId);
            audit.setOwnerId(ownerId);
            audit.setCommander("Cmd");
            audit.setColorIdentity("G");
            audit.setBracket("casual");
            audit.setArchetype("ramp");
            audit.setAlgorithmVersion("test-v1");
            audit.setCreatedAt(OffsetDateTime.now());
            audit.setRecommendationsJson("[]");
            auditRepository.persist(audit);
        });
    }

    private void importCollection(String content) {
        given()
                .contentType(ContentType.JSON)
                .body("{\"content\":\"" + content.replace("\n", "\\n") + "\",\"replaceExisting\":true}")
                .when().post("/users/me/collection/import")
                .then()
                .statusCode(200);
    }

    private Long idFromLocation(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private Map<String, CardResponseDTO> resolvedCards(List<String> names) {
        Map<String, CardResponseDTO> cards = new LinkedHashMap<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            cards.put(normalize(name), cardFor(name));
        }
        return cards;
    }

    private CardResponseDTO cardFor(String name) {
        String normalized = normalize(name);
        if ("cmd".equals(normalized)) {
            return new CardResponseDTO(name.trim(), "{2}{G}", "Legendary Creature - Elf", "", 3.0, List.of("G"), List.of());
        }
        return new CardResponseDTO(name.trim(), "", "", "", 0.0, List.of(), List.of());
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
