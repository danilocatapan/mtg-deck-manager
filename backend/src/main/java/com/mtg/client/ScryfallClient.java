package com.mtg.client;

import com.mtg.dto.ScryfallResponseDTO;
import com.mtg.dto.ScryfallCollectionRequestDTO;
import com.mtg.dto.ScryfallCollectionResponseDTO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/cards")
@RegisterRestClient(configKey = "scryfall-api")
@ClientHeaderParam(name = "User-Agent", value = "mtg-deck-manager/1.0")
@ClientHeaderParam(name = "Accept", value = "application/json;q=0.9,*/*;q=0.8")
public interface ScryfallClient {

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    ScryfallResponseDTO searchByName(@QueryParam("q") String query);

    @POST
    @Path("/collection")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ScryfallCollectionResponseDTO collection(ScryfallCollectionRequestDTO request);
}
