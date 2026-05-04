package com.mtg.client;

import com.mtg.dto.ScryfallCardResponseDTO;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/cards/named")
@RegisterRestClient(configKey = "scryfall-api")
public interface ScryfallClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ScryfallCardResponseDTO findByName(@QueryParam("fuzzy") String name);
}
