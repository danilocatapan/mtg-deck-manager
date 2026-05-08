package com.mtg.controller;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;

@QuarkusTest
class AppInfoResourceTest {

    @Test
    void appInfoReturnsPublicBuildMetadata() {
        given()
                .when()
                .get("/app/info")
                .then()
                .statusCode(200)
                .body("name", equalTo("MTG Deck Manager API"))
                .body("version", not(blankOrNullString()))
                .body("commit", not(blankOrNullString()))
                .body("branch", not(blankOrNullString()))
                .body("buildTime", not(blankOrNullString()))
                .body("environment", not(blankOrNullString()))
                .body("creator", equalTo("Danilo Catapan"))
                .body("objective", equalTo("Analise e otimizacao de decks Commander com recomendacoes explicaveis."));
    }
}
