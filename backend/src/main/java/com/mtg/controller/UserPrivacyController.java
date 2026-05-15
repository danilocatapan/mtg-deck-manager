package com.mtg.controller;

import com.mtg.dto.UserDataExportDTO;
import com.mtg.service.AuthenticatedUserService;
import com.mtg.service.UserPrivacyService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

@Path("/users/me")
@Produces(MediaType.APPLICATION_JSON)
public class UserPrivacyController {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    AuthenticatedUserService authenticatedUserService;

    @Inject
    UserPrivacyService userPrivacyService;

    @GET
    @Path("/export")
    @Authenticated
    @Operation(summary = "Export authenticated user data")
    @APIResponse(responseCode = "200", description = "User data exported as JSON")
    public UserDataExportDTO exportData() {
        return userPrivacyService.exportData(authenticatedUserService.profile(securityIdentity));
    }

    @DELETE
    @Authenticated
    @Operation(summary = "Delete authenticated user account data")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "User decks and related data deleted"),
            @APIResponse(responseCode = "401", description = "Authentication required")
    })
    public Response deleteAccount() {
        userPrivacyService.deleteAccountData(authenticatedUserService.subject(securityIdentity));
        return Response.noContent().build();
    }
}
