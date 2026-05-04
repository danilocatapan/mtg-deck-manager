package com.mtg.controller;

import com.mtg.dto.CardResponseDTO;
import com.mtg.service.CardService;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/cards")
@Produces(MediaType.APPLICATION_JSON)
public class CardController {

    private final CardService cardService;

    @Inject
    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GET
    public CardResponseDTO findByName(@QueryParam("name") String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Query parameter 'name' is required");
        }
        return cardService.findByName(name);
    }
}
