package com.mtg.controller;

import com.mtg.dto.ErrorResponseDTO;
import com.mtg.dto.RecommendationAuditFeedbackDTO;
import com.mtg.service.RecommendationAuditService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/recommendation-audits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecommendationAuditController {

    @Inject
    RecommendationAuditService auditService;

    @Inject
    SecurityIdentity securityIdentity;

    @POST
    @Path("{id}/feedback")
    @Authenticated
    @Operation(summary = "Register human feedback for a recommendation audit run")
    public Response feedback(@PathParam("id") String idStr, RecommendationAuditFeedbackDTO dto) {
        Long id = parseId(idStr);
        if (id == null) {
            return badRequest("Invalid audit id");
        }
        try {
            auditService.updateFeedback(id, dto, currentUserId());
            return Response.noContent().build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        } catch (NotFoundException exception) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private Long parseId(String idStr) {
        try {
            long id = Long.parseLong(idStr);
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Response badRequest(String message) {
        String safeMessage = message == null || message.isBlank() ? "Invalid request" : message;
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponseDTO(safeMessage))
                .build();
    }

    private String currentUserId() {
        return securityIdentity.getPrincipal().getName();
    }
}
