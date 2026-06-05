package com.mtg.controller;

import com.mtg.domain.DeckRecommendations;
import com.mtg.domain.RecommendationCoverage;
import com.mtg.domain.RecommendationItem;
import com.mtg.domain.RecommendationSourceSummary;
import com.mtg.domain.StrategicRecommendation;
import com.mtg.domain.StrategicRecommendationRun;
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
    void postStrategicRecommendationsReturnsQualityGateObject() {
        List<StrategicRecommendation> recommendations = List.of(
                new StrategicRecommendation("Greater Good melhora o plano e Arcane Encyclopedia e o corte correto.", "Greater Good", "Arcane Encyclopedia"),
                new StrategicRecommendation("Nature's Lore melhora a curva e Nissa's Pilgrimage e o corte correto.", "Nature's Lore", "Nissa's Pilgrimage"),
                new StrategicRecommendation("Heroic Intervention protege o comandante e Rhonas's Monument e o corte correto.", "Heroic Intervention", "Rhonas's Monument")
        );
        when(strategicRecommendationService.recommendRun(1L, new RecommendationParamsDTO(null, "casual", null, null), "google-user-1")).thenReturn(new StrategicRecommendationRun(
                "medium_confidence",
                new RecommendationCoverage(true, false, 0, List.of(), 8, 8, 1.0, 99, "casual", true),
                "unknown",
                new RecommendationSourceSummary("auto", 0, List.of(), "Sem fonte meta suficiente; heuristicas locais e regras Commander.", false, true),
                List.of("Dados meta insuficientes para este comandante/bracket; a recomendacao usa heuristicas conservadoras."),
                "not_proven_against_gpt",
                42L,
                recommendations
        ));

        String body = given().contentType(ContentType.JSON)
                .body(new RecommendationParamsDTO(null, "casual", null, null))
                .when().post("/decks/1/recommendations/strategic")
                .then()
                .statusCode(200)
                .body("confidence", org.hamcrest.Matchers.equalTo("medium_confidence"))
                .body("coverage.fallbackUsed", org.hamcrest.Matchers.equalTo(true))
                .body("limitations[0]", org.hamcrest.Matchers.containsString("Dados meta insuficientes"))
                .body("auditId", org.hamcrest.Matchers.equalTo(42))
                .body("recommendations.size()", org.hamcrest.Matchers.equalTo(3))
                .body("recommendations[0].add", org.hamcrest.Matchers.equalTo("Greater Good"))
                .body("recommendations[0].remove", org.hamcrest.Matchers.equalTo("Arcane Encyclopedia"))
                .body("recommendations[0].source", org.hamcrest.Matchers.equalTo("heuristic_fallback"))
                .body("recommendations[0].confidence", org.hamcrest.Matchers.equalTo("medium"))
                .body("recommendations[0].problem", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))
                .body("recommendations[0].risk", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()))
                .extract().asString();

        assertTrue(body.startsWith("{\"confidence\""));
    }

    @Test
    void postStrategicRecommendationsRequiresAuthentication() {
        given().contentType(ContentType.JSON)
                .body(new RecommendationParamsDTO(null, "casual", null, null))
                .when().post("/decks/1/recommendations/strategic")
                .then().statusCode(401);
    }
}
