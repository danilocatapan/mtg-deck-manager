package com.mtg.controller;

import com.mtg.domain.DeckAnalysis;
import com.mtg.service.DeckAnalysisService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@QuarkusTest
class DeckControllerAnalysisTest {

    @InjectMock
    DeckAnalysisService analysisService;

    @Test
    void getAnalysis() {
        DeckAnalysis analysis = new DeckAnalysis(2.4, 100, 10, 8, 7, Map.of(1,12,2,25,3,20,4,15));
        when(analysisService.analyzeDeck(1L)).thenReturn(analysis);

        given().when().get("/decks/1/analysis")
                .then().statusCode(200)
                .body("averageCmc", equalTo(2.4f))
                .body("totalCards", equalTo(100))
                .body("rampCount", equalTo(10));
    }

    @Test
    void notFound() {
        when(analysisService.analyzeDeck(999L)).thenThrow(new jakarta.ws.rs.NotFoundException());

        given().when().get("/decks/999/analysis").then().statusCode(404);
    }
}
