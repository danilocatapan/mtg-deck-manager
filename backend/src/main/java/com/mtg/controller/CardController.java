package com.mtg.controller;

import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.ErrorResponseDTO;
import com.mtg.service.CardService;
import com.mtg.service.ExternalServiceException;
import com.mtg.service.RateLimitedExternalServiceException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/cards")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Cards")
public class CardController {

    private static final Logger LOG = Logger.getLogger(CardController.class);

    private final CardService cardService;

    @Inject
    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GET
    @Operation(summary = "Search cards by name", description = "Searches the Scryfall API and returns matching cards.")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Cards found successfully",
                    content = @Content(schema = @Schema(implementation = CardResponseDTO.class, type = SchemaType.ARRAY))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid card name",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class, type = SchemaType.OBJECT))
            ),
            @APIResponse(
                    responseCode = "429",
                    description = "Scryfall rate limit reached",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class, type = SchemaType.OBJECT))
            ),
            @APIResponse(
                    responseCode = "502",
                    description = "Failed to query Scryfall",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class, type = SchemaType.OBJECT))
            )
    })
    public RestResponse<?> findByName(
            @Parameter(description = "Card name to search for", required = true)
            @QueryParam("name") String name
    ) {
        LOG.infov("event=cards.endpoint.request name={0}", name);

        if (name == null || name.isBlank()) {
            return RestResponse.status(
                    RestResponse.Status.BAD_REQUEST,
                    new ErrorResponseDTO("Query parameter 'name' is required")
            );
        }

        try {
            List<CardResponseDTO> cards = cardService.searchByName(name);
            return RestResponse.ok(cards);
        } catch (RateLimitedExternalServiceException exception) {
            LOG.warnv("event=cards.endpoint.rate_limited name={0}", name);
            return RestResponse.status(
                    RestResponse.Status.TOO_MANY_REQUESTS,
                    new ErrorResponseDTO("Scryfall rate limit reached. Please try again shortly.")
            );
        } catch (ExternalServiceException exception) {
            LOG.errorv(exception, "event=cards.endpoint.failure name={0}", name);
            return RestResponse.status(
                    RestResponse.Status.BAD_GATEWAY,
                    new ErrorResponseDTO("Unable to fetch cards from Scryfall")
            );
        }
    }
}
