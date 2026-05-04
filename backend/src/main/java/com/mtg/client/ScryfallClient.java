package com.mtg.client;

import com.mtg.dto.ScryfallResponseDTO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/cards")
@RegisterRestClient(configKey = "scryfall-api")
public interface ScryfallClient {

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    ScryfallResponseDTO searchByName(@QueryParam("q") String query);
}
