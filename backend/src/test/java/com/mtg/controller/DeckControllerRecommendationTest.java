package com.mtg.controller;

import com.mtg.domain.DeckRecommendations;
import com.mtg.domain.RecommendationItem;
import com.mtg.dto.RecommendationParamsDTO;
import com.mtg.service.RecommendationService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import static org.mockito.Mockito.when;

@QuarkusTest
class DeckControllerRecommendationTest {

    @InjectMock
    RecommendationService recommendationService;

    @Test
    void postRecommendations() {
        DeckRecommendations rec = new DeckRecommendations(List.of(new RecommendationItem("Arcane Signet","ramp","gap ramp",0.8,0.5,0.2,0.1,10.0)), List.of(), java.util.Map.of("ramp",3));
        when(recommendationService.recommend(1L, new RecommendationParamsDTO(null,null,null,null))).thenReturn(rec);

        given().contentType(ContentType.JSON).body(new RecommendationParamsDTO(null,null,null,null)).when().post("/decks/1/recommendations")
            .then().statusCode(200);
    }
}
