package com.mtg.client;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "commander-spellbook-api")
@Path("/api")
public interface CommanderSpellbookClient {
    @GET
    @Path("/variants/")
    @Produces(MediaType.APPLICATION_JSON)
    JsonNode searchVariants(@QueryParam("q") String query, @QueryParam("limit") Integer limit);
}
