package com.mtg.controller;

import com.mtg.dto.CardResponseDTO;
import com.mtg.service.CardService;
import com.mtg.service.ExternalServiceException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
class CardControllerTest {

    @InjectMock
    CardService cardService;

    @Test
    void shouldReturnCardsForAValidName() {
        when(cardService.searchByName("Sol Ring")).thenReturn(List.of(
                new CardResponseDTO("Sol Ring", "{1}", "Artifact", "{T}: Add {C}{C}.", 1.0)
        ));

        given()
                .queryParam("name", "Sol Ring")
                .when()
                .get("/cards")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].name", equalTo("Sol Ring"))
                .body("[0].manaCost", equalTo("{1}"))
                .body("[0].typeLine", equalTo("Artifact"));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsMissing() {
        given()
                .when()
                .get("/cards")
                .then()
                .statusCode(400)
                .body("message", equalTo("Query parameter 'name' is required"));
    }

    @Test
    void shouldReturnBadGatewayWhenServiceFails() {
        when(cardService.searchByName("Broken Card")).thenThrow(
                new ExternalServiceException("Failed to fetch cards from Scryfall", new RuntimeException("boom"))
        );

        given()
                .queryParam("name", "Broken Card")
                .when()
                .get("/cards")
                .then()
                .statusCode(502)
                .body("message", equalTo("Unable to fetch cards from Scryfall"));
    }

    @Test
    void shouldDocumentCardsEndpointInOpenApi() {
        given()
                .when()
                .get("/swagger")
                .then()
                .statusCode(200)
                .body(containsString("/cards"))
                .body(containsString("Search cards by name"));
    }
}

