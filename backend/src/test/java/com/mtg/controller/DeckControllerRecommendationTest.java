package com.mtg.controller;

import com.mtg.domain.DeckRecommendations;
import com.mtg.domain.RecommendationItem;
import com.mtg.domain.StrategicRecommendation;
import com.mtg.dto.RecommendationParamsDTO;
import com.mtg.service.RecommendationService;
import com.mtg.service.StrategicRecommendationService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@QuarkusTest
class DeckControllerRecommendationTest {

    @InjectMock
    RecommendationService recommendationService;

    @InjectMock
    StrategicRecommendationService strategicRecommendationService;

    @Test
    @TestSecurity(user = "google-user-1")
    void postRecommendations() {
        DeckRecommendations rec = new DeckRecommendations(List.of(new RecommendationItem("Arcane Signet","ramp","gap ramp",0.8,0.5,0.2,0.1,10.0)), List.of(), java.util.Map.of("ramp",3));
        when(recommendationService.recommend(1L, new RecommendationParamsDTO(null,null,null,null), "google-user-1")).thenReturn(rec);

        given().contentType(ContentType.JSON).body(new RecommendationParamsDTO(null,null,null,null)).when().post("/decks/1/recommendations")
            .then().statusCode(200);
    }

    @Test
    void postRecommendationsRequiresAuthentication() {
        given().contentType(ContentType.JSON)
                .body(new RecommendationParamsDTO(null, null, null, null))
                .when().post("/decks/1/recommendations")
                .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void postStrategicRecommendationsReturnsJsonArray() {
        when(strategicRecommendationService.recommend(1L, new RecommendationParamsDTO(null, "casual", null, null), "google-user-1")).thenReturn(List.of(
                new StrategicRecommendation("Greater Good melhora o plano e Arcane Encyclopedia e o corte correto.", "Greater Good", "Arcane Encyclopedia"),
                new StrategicRecommendation("Nature's Lore melhora a curva e Nissa's Pilgrimage e o corte correto.", "Nature's Lore", "Nissa's Pilgrimage"),
                new StrategicRecommendation("Heroic Intervention protege o comandante e Rhonas's Monument e o corte correto.", "Heroic Intervention", "Rhonas's Monument")
        ));

        String body = given().contentType(ContentType.JSON)
                .body(new RecommendationParamsDTO(null, "casual", null, null))
                .when().post("/decks/1/recommendations/strategic")
                .then()
                .statusCode(200)
                .body("size()", org.hamcrest.Matchers.equalTo(3))
                .body("[0].add", org.hamcrest.Matchers.equalTo("Greater Good"))
                .body("[0].remove", org.hamcrest.Matchers.equalTo("Arcane Encyclopedia"))
                .body("[0].source", org.hamcrest.Matchers.equalTo("heuristic_fallback"))
                .body("[0].confidence", org.hamcrest.Matchers.equalTo("medium"))
                .extract().asString();

        assertTrue(body.startsWith("[{\"reasoning\""));
    }

    @Test
    void postStrategicRecommendationsRequiresAuthentication() {
        given().contentType(ContentType.JSON)
                .body(new RecommendationParamsDTO(null, "casual", null, null))
                .when().post("/decks/1/recommendations/strategic")
                .then().statusCode(401);
    }
}
