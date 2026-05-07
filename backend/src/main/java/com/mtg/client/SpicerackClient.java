package com.mtg.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "spicerack-api")
@Path("/api/export-decklists/")
public interface SpicerackClient {

    @GET
    List<SpicerackTournamentDTO> exportDecklists(
            @HeaderParam("X-API-Key") String apiKey,
            @QueryParam("num_days") int numDays,
            @QueryParam("event_format") String eventFormat,
            @QueryParam("decklist_as_text") boolean decklistAsText
    );
}
