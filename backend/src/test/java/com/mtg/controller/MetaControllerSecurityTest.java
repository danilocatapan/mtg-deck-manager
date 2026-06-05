package com.mtg.controller;

import com.mtg.service.meta.CommanderMetaProfileService;
import com.mtg.service.meta.ExternalMetaIngestionJob;
import com.mtg.service.meta.MetaProvider;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.SecurityAttribute;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

@QuarkusTest
class MetaControllerSecurityTest {

    @InjectMock
    MetaProvider metaProvider;

    @InjectMock
    ExternalMetaIngestionJob ingestionJob;

    @InjectMock
    CommanderMetaProfileService profileService;

    @Test
    void syncIsForbiddenWhenAdminKeyIsNotConfigured() {
        given().when().post("/meta/sync").then().statusCode(403);
    }

    @Test
    void rebuildProfilesIsForbiddenWhenAdminKeyIsNotConfigured() {
        given().when().post("/meta/rebuild-profiles").then().statusCode(403);
    }

    @Test
    void cachedMetaDecksAreForbiddenWhenAdminKeyIsNotConfigured() {
        given().when().get("/meta/decks").then().statusCode(403);
    }

    @Test
    void benchmarkAdminEndpointIsForbiddenWhenAdminKeyIsNotConfigured() {
        given().when().get("/meta/recommendation-benchmark/summary").then().statusCode(403);
        given().when().post("/meta/recommendation-benchmark/run").then().statusCode(403);
        given().when().get("/meta/recommendation-benchmark/reviews/next").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "meta-admin-user", attributes = {
            @SecurityAttribute(key = "email", value = "dcatapan@gmail.com")
    })
    void configuredGoogleAdminCanSyncAndReadDerivedBenchmarkActions() {
        when(ingestionJob.sync()).thenReturn(new com.mtg.dto.MetaSyncSummaryDTO(
                "success", 12, 0, 12, 4, java.util.Map.of("cedh", 12), 4, java.util.List.of(), java.util.List.of(),
                new com.mtg.service.meta.MetaSourceStatus("TopDeck", true, java.time.OffsetDateTime.now(), java.util.List.of("cedh"), "competitive_meta")
        ));

        given().when().post("/meta/sync")
                .then().statusCode(200)
                .body("status", is("success"))
                .body("importedDecks", is(12));

        given().when().get("/meta/recommendation-benchmark/summary")
                .then().statusCode(200)
                .body("nextActions[0].id", is("expand-corpus"));
    }
}
