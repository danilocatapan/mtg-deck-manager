package com.mtg.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class SecurityResourceTest {

    @Test
    void checkRequiresAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/security/status/check")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "regular-user")
    void checkRequiresAdminSubject() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/security/status/check")
                .then()
                .statusCode(403)
                .body("message", equalTo("Admin permission is required"));
    }

    @Test
    @TestSecurity(user = "security-admin-user")
    void checkReturnsRedactedReadOnlySecurityStatusForConfiguredAdmin() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "includeDetails": true,
                          "scanExternalDependencies": true
                        }
                        """)
                .when().post("/security/status/check")
                .then()
                .statusCode(200)
                .header("Cache-Control", containsString("no-store"))
                .header("Content-Security-Policy", containsString("default-src 'none'"))
                .body("status", equalTo("warning"))
                .body("environment", equalTo("local"))
                .body("issues.type", hasItem("OIDC_CLIENT_ID"))
                .body("issues.type", hasItem("DEPENDENCY_SCAN_RUNTIME"))
                .body("details.auth.clientIdConfigured", equalTo(false))
                .body("details.dependencyScan.runtimeExternalCalls", equalTo(false))
                .body("details.auth.sensitiveValues", equalTo("redacted"));
    }

    @Test
    @TestSecurity(user = "role-admin-user", roles = "admin")
    void checkAllowsAdminRole() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/security/status/check")
                .then()
                .statusCode(200)
                .body("generatedAt", not(equalTo("")));
    }

    @Test
    @TestSecurity(user = "security-admin-user")
    void checkRejectsUnknownPayloadFields() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "includeDetails": true,
                          "rawConfig": true
                        }
                        """)
                .when().post("/security/status/check")
                .then()
                .statusCode(400)
                .body("message", equalTo("Unknown security status field: rawConfig"));
    }

    @Test
    @TestSecurity(user = "security-admin-user")
    void checkRejectsNonBooleanPayloadFields() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "includeDetails": "yes"
                        }
                        """)
                .when().post("/security/status/check")
                .then()
                .statusCode(400)
                .body("message", equalTo("includeDetails must be a boolean"));
    }
}
