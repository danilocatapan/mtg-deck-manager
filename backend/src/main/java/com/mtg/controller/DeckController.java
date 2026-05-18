package com.mtg.controller;

import com.mtg.domain.DeckAnalysis;
import com.mtg.domain.DeckRecommendations;
import com.mtg.domain.StrategicRecommendation;
import com.mtg.config.SensitiveLogSanitizer;
import com.mtg.dto.ApplyRecommendationSwapDTO;
import com.mtg.dto.AuthenticatedUserDTO;
import com.mtg.dto.DeckConsultResponseDTO;
import com.mtg.dto.DeckLegalityDTO;
import com.mtg.dto.DeckImportDTO;
import com.mtg.dto.DeckRequestDTO;
import com.mtg.dto.DeckResponseDTO;
import com.mtg.dto.ErrorResponseDTO;
import com.mtg.dto.PublicDeckSummaryDTO;
import com.mtg.dto.RecommendationParamsDTO;
import com.mtg.dto.SimilarDeckComparisonDTO;
import com.mtg.config.StructuredRestLog;
import com.mtg.service.AuthenticatedUserService;
import com.mtg.service.DeckAnalysisService;
import com.mtg.service.DeckComparisonService;
import com.mtg.service.DeckLegalityService;
import com.mtg.service.DeckService;
import com.mtg.service.RecommendationService;
import com.mtg.service.StrategicRecommendationService;
import jakarta.inject.Inject;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.logging.Logger;

@Path("/decks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeckController {
    private static final Logger LOG = Logger.getLogger(DeckController.class);

    @Inject
    DeckService deckService;

    @Inject
    AuthenticatedUserService authenticatedUserService;

    @Inject
    DeckAnalysisService deckAnalysisService;

    @Inject
    DeckLegalityService deckLegalityService;

    @Inject
    DeckComparisonService deckComparisonService;

    @Inject
    RecommendationService recommendationService;

    @Inject
    StrategicRecommendationService strategicRecommendationService;

    @Inject
    SecurityIdentity securityIdentity;

    @POST
    @Authenticated
    @Operation(summary = "Create a new deck")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Deck created")
    })
    public Response createDeck(DeckRequestDTO request) {
        try {
            DeckResponseDTO created = deckService.createDeck(request, currentUserProfile());
            URI location = URI.create("/decks/" + created.id());
            return Response.created(location).entity(created).build();
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @POST
    @Path("/import")
    @Authenticated
    @Operation(summary = "Import deck from text")
    public Response importDeck(DeckImportDTO dto) {
        try {
            DeckResponseDTO created = deckService.importDeck(dto, currentUserProfile());
            URI location = URI.create("/decks/" + created.id());
            return Response.created(location).entity(created).build();
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @GET
    @Authenticated
    @Operation(summary = "List decks")
    public List<DeckResponseDTO> listDecks() {
        return deckService.listDecks(currentUserId());
    }

    @GET
    @Path("/public")
    @Operation(summary = "List public decks")
    public List<PublicDeckSummaryDTO> listPublicDecks(
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @QueryParam("commander") String commander
    ) {
        return deckService.listPublicDecks(page, size, commander);
    }

    @GET
    @Path("{id}")
    @Authenticated
    @Operation(summary = "Get deck by id")
    public Response getDeck(@Parameter(description = "Deck id") @PathParam("id") String idStr) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        DeckResponseDTO dto = deckService.getDeckById(id, currentUserId());
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dto).build();
    }

    @GET
    @Path("{id}/consult")
    @Operation(summary = "Consult a deck in read-only mode")
    public Response consultDeck(@Parameter(description = "Deck id") @PathParam("id") String idStr) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        DeckConsultResponseDTO dto = deckService.consultDeck(id, currentUserIdOrNull());
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dto).build();
    }

    @PUT
    @Path("{id}")
    @Authenticated
    @Operation(summary = "Update a deck")
    public Response updateDeck(@PathParam("id") String idStr, DeckRequestDTO request) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        DeckResponseDTO updated;
        try {
            updated = deckService.updateDeck(id, request, currentUserProfile());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }

        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("{id}")
    @Authenticated
    @Operation(summary = "Delete a deck")
    public Response deleteDeck(@PathParam("id") String idStr) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        boolean deleted = deckService.deleteDeck(id, currentUserId());
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("{id}/export")
    @Produces(MediaType.TEXT_PLAIN)
    @Authenticated
    @Operation(summary = "Export deck in text format")
    public Response exportDeck(@PathParam("id") String idStr) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        String exported = deckService.exportDeck(id, currentUserId());
        if (exported == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(exported).type("text/plain;charset=UTF-8").build();
    }

    @GET
    @Path("{id}/analysis")
    @Authenticated
    @Operation(summary = "Analyze deck")
    @APIResponse(responseCode = "200", description = "Deck analysis")
    public Response analyzeDeck(@Parameter(description = "Deck id") @PathParam("id") String idStr) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        try {
            DeckAnalysis analysis = deckAnalysisService.analyzeDeck(id, currentUserId());
            return Response.ok(analysis).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("{id}/legality")
    @Authenticated
    @Operation(summary = "Check Commander deck legality")
    @APIResponse(responseCode = "200", description = "Commander legality report")
    public Response legality(@Parameter(description = "Deck id") @PathParam("id") String idStr) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        try {
            DeckLegalityDTO legality = deckLegalityService.check(id, currentUserId());
            return Response.ok(legality).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("{id}/recommendations")
    @Authenticated
    @Operation(summary = "Recommend deck improvements")
    @APIResponse(responseCode = "200", description = "Recommendations generated")
    public Response recommend(@PathParam("id") String idStr, RecommendationParamsDTO params) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        try {
            DeckRecommendations recs = recommendationService.recommend(id, params, currentUserId());
            return Response.ok(recs).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("{id}/recommendations/strategic")
    @Authenticated
    @Operation(summary = "Recommend strategic deck improvements")
    @APIResponse(responseCode = "200", description = "Strategic recommendations generated")
    public Response strategicRecommendations(@PathParam("id") String idStr, RecommendationParamsDTO params) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        try {
            List<StrategicRecommendation> recommendations = strategicRecommendationService.recommend(id, params, currentUserId());
            return Response.ok(recommendations).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            return badRequest(e.getMessage());
        }
    }

    @POST
    @Path("{id}/recommendations/apply-swap")
    @Authenticated
    @Operation(summary = "Apply a recommended deck swap")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Swap applied"),
            @APIResponse(responseCode = "400", description = "Invalid swap"),
            @APIResponse(responseCode = "404", description = "Deck not found")
    })
    public Response applyRecommendationSwap(@PathParam("id") String idStr, ApplyRecommendationSwapDTO dto) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        try {
            DeckResponseDTO updated = deckService.applyRecommendationSwap(id, dto, currentUserId());
            if (updated == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @POST
    @Path("{id}/recommendations/undo-swap")
    @Authenticated
    @Operation(summary = "Undo a recommended deck swap")
    public Response undoRecommendationSwap(@PathParam("id") String idStr, ApplyRecommendationSwapDTO dto) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        try {
            DeckResponseDTO updated = deckService.undoRecommendationSwap(id, dto == null ? null : dto.recommendationId(), currentUserId());
            if (updated == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(updated).build();
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @POST
    @Path("{id}/comparison")
    @Authenticated
    @Operation(summary = "Compare deck against similar commander decks")
    public Response compareToSimilar(@PathParam("id") String idStr, RecommendationParamsDTO params) {
        Long id = parseDeckId(idStr);
        if (id == null) return badRequest("Invalid deck id");

        try {
            SimilarDeckComparisonDTO comparison = deckComparisonService.compare(id, params, currentUserId());
            return Response.ok(comparison).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private Long parseDeckId(String idStr) {
        try {
            long id = Long.parseLong(idStr);
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Response badRequest(String message) {
        String safeMessage = message == null || message.isBlank() ? "Invalid request" : message;
        StructuredRestLog.validation(
                LOG,
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Requisicao rejeitada por violacao de regra de negocio.",
                "INVALID_DECK_REQUEST",
                null,
                SensitiveLogSanitizer.reasonCode(safeMessage)
        );
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponseDTO(safeMessage))
                .build();
    }

    private String currentUserId() {
        return currentUserProfile().googleSubject();
    }

    private String currentUserIdOrNull() {
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            return null;
        }
        try {
            return authenticatedUserService.subject(securityIdentity);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private AuthenticatedUserDTO currentUserProfile() {
        return authenticatedUserService.profile(securityIdentity);
    }
}
