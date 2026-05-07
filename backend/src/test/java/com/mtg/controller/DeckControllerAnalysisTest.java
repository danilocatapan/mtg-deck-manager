package com.mtg.controller;

import com.mtg.domain.DeckAnalysis;
import com.mtg.service.DeckAnalysisService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class DeckControllerAnalysisTest {

    @InjectMock
    DeckAnalysisService analysisService;

    @Test
    @TestSecurity(user = "google-user-1")
    void getAnalysis() {
        DeckAnalysis analysis = new DeckAnalysis(2.4, 100, 10, 8, 7, Map.of(1,12,2,25,3,20,4,15));
        when(analysisService.analyzeDeck(1L, "google-user-1")).thenReturn(analysis);

        given().when().get("/decks/1/analysis")
                .then().statusCode(200)
                .body("averageCmc", equalTo(2.4f))
                .body("totalCards", equalTo(100))
                .body("rampCount", equalTo(10));

        verify(analysisService).analyzeDeck(1L, "google-user-1");
    }

    @Test
    void getAnalysisRequiresAuthentication() {
        given().when().get("/decks/1/analysis").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void notFound() {
        when(analysisService.analyzeDeck(999L, "google-user-1")).thenThrow(new jakarta.ws.rs.NotFoundException());

        given().when().get("/decks/999/analysis").then().statusCode(404);
    }
}
