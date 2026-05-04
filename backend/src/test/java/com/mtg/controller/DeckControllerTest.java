package com.mtg.controller;

import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.DeckRequestDTO;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
class DeckControllerTest {

    @Test
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
    void getNotFound() {
        given().when().get("/decks/999999").then().statusCode(404);
    }
}
