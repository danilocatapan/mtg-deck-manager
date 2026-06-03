package com.mtg.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.rest.client.reactive.NotBody;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "topdeck-api")
public interface TopDeckClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ClientHeaderParam(name = "Authorization", value = "{apiKey}")
    List<TopDeckTournamentDTO> tournaments(
            @NotBody String apiKey,
            TopDeckTournamentRequestDTO request
    );
}
