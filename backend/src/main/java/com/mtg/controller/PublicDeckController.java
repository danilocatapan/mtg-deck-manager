package com.mtg.controller;

import com.mtg.dto.AuthenticatedUserDTO;
import com.mtg.dto.DeckResponseDTO;
import com.mtg.dto.PublicDeckResponseDTO;
import com.mtg.dto.PublicDeckSummaryDTO;
import com.mtg.service.AuthenticatedUserService;
import com.mtg.service.DeckService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/public/decks")
@Produces(MediaType.APPLICATION_JSON)
public class PublicDeckController {

    @Inject
    DeckService deckService;

    @Inject
    AuthenticatedUserService authenticatedUserService;

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Operation(summary = "List public decks")
    public List<PublicDeckSummaryDTO> listPublicDecks(
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @QueryParam("commander") String commander
    ) {
        return deckService.listPublicDecks(page, size, commander, currentOwnerId());
    }

    @GET
    @Path("top")
    @Operation(summary = "List top public decks by internal likes")
    public Response listTopPublicDecks(
            @QueryParam("period") String period,
            @QueryParam("size") Integer size
    ) {
        try {
            return Response.ok(deckService.listTopPublicDecks(period, size, currentOwnerId())).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Get public deck details")
    public Response getPublicDeck(@PathParam("id") String idStr) {
        Long id = parseDeckId(idStr);
        if (id == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        PublicDeckResponseDTO deck = deckService.getPublicDeck(id, currentOwnerId());
        if (deck == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(deck).build();
    }

    @POST
    @Path("{id}/copy")
    @Authenticated
    @Operation(summary = "Copy a public deck to the authenticated user's library")
    public Response copyPublicDeck(@PathParam("id") String idStr) {
        Long id = parseDeckId(idStr);
        if (id == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        AuthenticatedUserDTO user = authenticatedUserService.profile(securityIdentity);
        DeckResponseDTO copied = deckService.copyPublicDeck(id, user);
        if (copied == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.CREATED).entity(copied).build();
    }

    @POST
    @Path("{id}/like")
    @Authenticated
    @Operation(summary = "Like a public deck")
    public Response likePublicDeck(@PathParam("id") String idStr) {
        Long id = parseDeckId(idStr);
        if (id == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        PublicDeckResponseDTO liked = deckService.likePublicDeck(id, authenticatedUserService.subject(securityIdentity));
        if (liked == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(liked).build();
    }

    @DELETE
    @Path("{id}/like")
    @Authenticated
    @Operation(summary = "Remove the authenticated user's like from a public deck")
    public Response unlikePublicDeck(@PathParam("id") String idStr) {
        Long id = parseDeckId(idStr);
        if (id == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        boolean removed = deckService.unlikePublicDeck(id, authenticatedUserService.subject(securityIdentity));
        if (!removed) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    private Long parseDeckId(String idStr) {
        try {
            long id = Long.parseLong(idStr);
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String currentOwnerId() {
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            return null;
        }
        try {
            return authenticatedUserService.subject(securityIdentity);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
