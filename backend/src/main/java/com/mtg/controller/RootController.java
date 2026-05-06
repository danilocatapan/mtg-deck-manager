package com.mtg.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class RootController {

    @ConfigProperty(name = "app.frontend.url", defaultValue = "https://danilocatapan.github.io/mtg-deck-manager/")
    String frontendUrl;

    @GET
    public Map<String, String> index() {
        return Map.of(
                "name", "MTG Deck Manager API",
                "status", "ok",
                "frontend", frontendUrl
        );
    }
}
