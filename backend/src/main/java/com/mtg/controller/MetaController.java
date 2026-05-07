package com.mtg.controller;

import com.mtg.service.meta.CommanderMetaProfile;
import com.mtg.service.meta.CommanderMetaProfileService;
import com.mtg.service.meta.ExternalMetaIngestionJob;
import com.mtg.service.meta.MetaDeck;
import com.mtg.service.meta.MetaProvider;
import com.mtg.service.meta.MetaSourceStatus;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/meta")
@Produces(MediaType.APPLICATION_JSON)
public class MetaController {

    private static final Logger LOG = Logger.getLogger(MetaController.class);
    private static final int MAX_COMMANDER_LENGTH = 120;
    private static final int MAX_OPTION_LENGTH = 40;

    @Inject
    MetaProvider metaProvider;

    @Inject
    ExternalMetaIngestionJob ingestionJob;

    @Inject
    CommanderMetaProfileService profileService;

    @ConfigProperty(name = "meta.sync.api-key")
    Optional<String> syncApiKey;

    @GET
    @Path("/sources")
    public Map<String, List<MetaSourceStatus>> sources() {
        return Map.of("sources", metaProvider.getSourceStatuses());
    }

    @POST
    @Path("/sync")
    public Response sync(@HeaderParam("X-Admin-Key") String adminKey) {
        if (!isAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(Map.of("sources", ingestionJob.sync())).build();
    }

    @GET
    @Path("/decks")
    public Response decks(@HeaderParam("X-Admin-Key") String adminKey) {
        if (!isAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(Map.of("decks", ingestionJob.cachedDecks())).build();
    }

    @POST
    @Path("/rebuild-profiles")
    public Response rebuildProfiles(@HeaderParam("X-Admin-Key") String adminKey) {
        if (!isAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(Map.of("profilesBuilt", profileService.rebuild())).build();
    }

    @GET
    @Path("/commanders/{commander}")
    public CommanderMetaProfile commander(
            @PathParam("commander") String commander,
            @QueryParam("bracket") String bracket,
            @QueryParam("sourceMode") String sourceMode
    ) {
        validateLength(commander, MAX_COMMANDER_LENGTH, "commander");
        validateLength(bracket, MAX_OPTION_LENGTH, "bracket");
        validateLength(sourceMode, MAX_OPTION_LENGTH, "sourceMode");
        return metaProvider.getCommanderProfile(commander, bracket, sourceMode);
    }

    private void validateLength(String value, int maxLength, String field) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long");
        }
    }

    private boolean isAdminAuthorized(String adminKey) {
        if (syncApiKey.isEmpty() || syncApiKey.get().isBlank()) {
            LOG.warn("event=meta.admin.denied reason=missing_admin_key_config");
            return false;
        }
        return syncApiKey.get().equals(adminKey);
    }
}
