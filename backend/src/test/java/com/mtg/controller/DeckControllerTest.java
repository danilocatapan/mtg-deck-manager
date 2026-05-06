package com.mtg.controller;

import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.DeckRequestDTO;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;

@QuarkusTest
class DeckControllerTest {

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
}
