package com.mtg.controller;

import com.mtg.service.meta.CommanderMetaProfileService;
import com.mtg.service.meta.ExternalMetaIngestionJob;
import com.mtg.service.meta.MetaProvider;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

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
}
