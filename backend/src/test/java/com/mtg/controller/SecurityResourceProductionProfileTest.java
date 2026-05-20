package com.mtg.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestProfile(SecurityResourceProductionProfileTest.ProductionSecurityProfile.class)
class SecurityResourceProductionProfileTest {

    @Test
    @TestSecurity(user = "security-admin-user")
    void checkRedactsDetailsInProduction() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "includeDetails": true,
                          "scanExternalDependencies": false
                        }
                        """)
                .when().post("/security/status/check")
                .then()
                .statusCode(200)
                .body("environment", equalTo("production"))
                .body("details.cors.origins", equalTo("redacted-in-production"))
                .body("details.auth.sensitiveValues", equalTo("redacted-in-production"));
    }

    public static class ProductionSecurityProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "app.environment", "production",
                    "security.admin.subjects", "security-admin-user",
                    "quarkus.http.cors.origins", "https://danilocatapan.github.io"
            );
        }
    }
}
