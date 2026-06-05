package com.mtg.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.rest.client.reactive.NotBody;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "openai-api")
public interface OpenAiResponsesClient {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ClientHeaderParam(name = "Authorization", value = "Bearer {apiKey}")
    JsonNode create(@NotBody String apiKey, JsonNode request);
}
