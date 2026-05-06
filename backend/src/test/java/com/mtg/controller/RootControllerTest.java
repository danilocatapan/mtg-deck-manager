package com.mtg.controller;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class RootControllerTest {

    @Test
    void indexReturnsApiMetadata() {
        given()
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .body("name", equalTo("MTG Deck Manager API"))
                .body("status", equalTo("ok"))
                .body("frontend", equalTo("https://danilocatapan.github.io/mtg-deck-manager/"));
    }
}
