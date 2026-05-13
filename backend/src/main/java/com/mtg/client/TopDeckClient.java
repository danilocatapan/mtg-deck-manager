package com.mtg.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "topdeck-api")
@Path("/api/v2/tournaments")
public interface TopDeckClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    List<TopDeckTournamentDTO> tournaments(
            @HeaderParam("Authorization") String apiKey,
            TopDeckTournamentRequestDTO request
    );
}
