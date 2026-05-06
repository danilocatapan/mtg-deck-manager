package com.mtg.controller;

import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.CardCollectionRequestDTO;
import com.mtg.dto.ErrorResponseDTO;
import com.mtg.service.CardService;
import com.mtg.service.ExternalServiceException;
import com.mtg.service.RateLimitedExternalServiceException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Cards")
public class CardController {

    private static final Logger LOG = Logger.getLogger(CardController.class);
    private static final int MAX_CARD_NAME_LENGTH = 120;
    private static final int MAX_COLLECTION_SIZE = 300;

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
        if (name.length() > MAX_CARD_NAME_LENGTH) {
            return RestResponse.status(
                    RestResponse.Status.BAD_REQUEST,
                    new ErrorResponseDTO("Query parameter 'name' is too long")
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

    @POST
    @Path("/collection")
    @Operation(summary = "Fetch cards by exact names", description = "Uses Scryfall collection lookup and local cache to resolve many cards with fewer upstream requests.")
    public RestResponse<?> findByNames(CardCollectionRequestDTO request) {
        if (request == null || request.names() == null || request.names().isEmpty()) {
            return RestResponse.status(
                    RestResponse.Status.BAD_REQUEST,
                    new ErrorResponseDTO("Body field 'names' is required")
            );
        }

        List<String> names = request.names().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();

        if (names.isEmpty()) {
            return RestResponse.status(
                    RestResponse.Status.BAD_REQUEST,
                    new ErrorResponseDTO("Body field 'names' must contain at least one card name")
            );
        }
        if (names.size() > MAX_COLLECTION_SIZE) {
            return RestResponse.status(
                    RestResponse.Status.BAD_REQUEST,
                    new ErrorResponseDTO("Body field 'names' accepts at most " + MAX_COLLECTION_SIZE + " cards")
            );
        }
        if (names.stream().anyMatch(name -> name.length() > MAX_CARD_NAME_LENGTH)) {
            return RestResponse.status(
                    RestResponse.Status.BAD_REQUEST,
                    new ErrorResponseDTO("Card names must be at most " + MAX_CARD_NAME_LENGTH + " characters")
            );
        }

        LOG.infov("event=cards.collection.endpoint.request count={0}", names.size());
        Map<String, CardResponseDTO> cardsByName = cardService.findByNames(names);
        List<CardResponseDTO> cards = names.stream()
                .map(name -> cardsByName.get(cardService.normalizeLookupName(name)))
                .filter(Objects::nonNull)
                .toList();
        return RestResponse.ok(cards);
    }
}
