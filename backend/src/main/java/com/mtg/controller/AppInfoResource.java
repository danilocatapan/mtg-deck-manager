package com.mtg.controller;

import com.mtg.dto.AppInfoResponseDTO;
import com.mtg.service.AppInfoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

@Path("/app/info")
@Produces(MediaType.APPLICATION_JSON)
public class AppInfoResource {

    @Inject
    AppInfoService appInfoService;

    @GET
    @Operation(summary = "Get application build metadata", description = "Returns public API version and build metadata for support and traceability.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Application metadata")
    })
    public AppInfoResponseDTO info() {
        return appInfoService.currentInfo();
    }
}
