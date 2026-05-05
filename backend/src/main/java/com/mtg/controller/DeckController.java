package com.mtg.controller;

import com.mtg.dto.DeckRequestDTO;
import com.mtg.dto.DeckResponseDTO;
import com.mtg.service.DeckService;
import com.mtg.service.DeckAnalysisService;
import com.mtg.domain.DeckAnalysis;
import com.mtg.service.RecommendationService;
import com.mtg.dto.RecommendationParamsDTO;
import com.mtg.domain.DeckRecommendations;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/decks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeckController {

    @Inject
    DeckService deckService;

    @Inject
    DeckAnalysisService deckAnalysisService;

    @Inject
    RecommendationService recommendationService;

    @POST
    @Operation(summary = "Create a new deck")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Deck created")
    })
    public Response createDeck(DeckRequestDTO request) {
        DeckResponseDTO created = deckService.createDeck(request);
        URI location = URI.create("/decks/" + created.id());
        return Response.created(location).entity(created).build();
    }

    @POST
    @Path("/import")
    @Operation(summary = "Import deck from text")
    public Response importDeck(com.mtg.dto.DeckImportDTO dto) {
        try {
            DeckResponseDTO created = deckService.importDeck(dto);
            URI location = URI.create("/decks/" + created.id());
            return Response.created(location).entity(created).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Operation(summary = "List decks")
    public List<DeckResponseDTO> listDecks() {
        return deckService.listDecks();
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Get deck by id")
    public Response getDeck(@Parameter(description = "Deck id") @PathParam("id") String idStr) {
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid deck id: " + idStr).build();
        }
        DeckResponseDTO dto = deckService.getDeckById(id);
        if (dto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(dto).build();
    }

    @PUT
    @Path("{id}")
    @Operation(summary = "Update a deck")
    public Response updateDeck(@PathParam("id") String idStr, DeckRequestDTO request) {
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid deck id: " + idStr).build();
        }
        DeckResponseDTO updated = deckService.updateDeck(id, request);
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("{id}")
    @Operation(summary = "Delete a deck")
    public Response deleteDeck(@PathParam("id") String idStr) {
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid deck id: " + idStr).build();
        }
        boolean deleted = deckService.deleteDeck(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("{id}/export")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Export deck in text format")
    public Response exportDeck(@PathParam("id") String idStr) {
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid deck id: " + idStr).build();
        }
        String exported = deckService.exportDeck(id);
        if (exported == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(exported).type("text/plain;charset=UTF-8").build();
    }

    @GET
    @Path("{id}/analysis")
    @Operation(summary = "Analyze deck")
    @APIResponse(responseCode = "200", description = "Deck analysis")
    public Response analyzeDeck(@Parameter(description = "Deck id") @PathParam("id") String idStr) {
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid deck id: " + idStr).build();
        }
        try {
            DeckAnalysis analysis = deckAnalysisService.analyzeDeck(id);
            return Response.ok(analysis).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("{id}/recommendations")
    @Operation(summary = "Recommend deck improvements")
    @APIResponse(responseCode = "200", description = "Recommendations generated")
    public Response recommend(@PathParam("id") String idStr, RecommendationParamsDTO params) {
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid deck id: " + idStr).build();
        }
        try {
            DeckRecommendations recs = recommendationService.recommend(id, params);
            return Response.ok(recs).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
