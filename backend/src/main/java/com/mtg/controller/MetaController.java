package com.mtg.controller;

import com.mtg.dto.ExternalDeckImportRequestDTO;
import com.mtg.dto.ExternalDeckImportResponseDTO;
import com.mtg.dto.MetaTopDeckImportRequestDTO;
import com.mtg.dto.MetaTopDeckImportResponseDTO;
import com.mtg.dto.MetaTopDeckSyncRequestDTO;
import com.mtg.service.AuthenticatedUserService;
import com.mtg.service.CommanderSpellbookComboSyncService;
import com.mtg.service.ExternalDeckImportService;
import com.mtg.service.MetaTopDeckService;
import com.mtg.service.meta.CommanderMetaProfile;
import com.mtg.service.meta.CommanderMetaProfileService;
import com.mtg.service.meta.ExternalMetaIngestionJob;
import com.mtg.service.meta.MetaDeck;
import com.mtg.service.meta.MetaProvider;
import com.mtg.service.meta.MetaSourceStatus;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

    @Inject
    ExternalDeckImportService externalDeckImportService;

    @Inject
    MetaTopDeckService metaTopDeckService;

    @Inject
    CommanderSpellbookComboSyncService comboSyncService;

    @Inject
    AuthenticatedUserService authenticatedUserService;

    @Inject
    SecurityIdentity securityIdentity;

    @ConfigProperty(name = "meta.sync.api-key")
    Optional<String> syncApiKey;

    @ConfigProperty(name = "meta.top-decks.admin-emails")
    Optional<String> topDeckAdminEmails;

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

    @POST
    @Path("/external-decks/import")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importExternalDecks(
            @HeaderParam("X-Admin-Key") String adminKey,
            ExternalDeckImportRequestDTO request
    ) {
        if (!isAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        ExternalDeckImportResponseDTO response = externalDeckImportService.importDecks(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/top-decks/import")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importTopDecks(
            @HeaderParam("X-Admin-Key") String adminKey,
            MetaTopDeckImportRequestDTO request
    ) {
        if (!isTopDeckAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        MetaTopDeckImportResponseDTO response = metaTopDeckService.importTopDecks(request);
        return Response.ok(response).build();
    }

    @GET
    @Path("/top-decks")
    public Response topDecks(
            @HeaderParam("X-Admin-Key") String adminKey,
            @QueryParam("source") String source,
            @QueryParam("rankingPeriod") String rankingPeriod,
            @QueryParam("rankingDate") java.time.LocalDate rankingDate,
            @QueryParam("format") String format,
            @QueryParam("commander") String commander,
            @QueryParam("archetype") String archetype,
            @QueryParam("bracket") String bracket,
            @QueryParam("colorIdentity") String colorIdentity,
            @QueryParam("limit") Integer limit
    ) {
        if (!isTopDeckAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(metaTopDeckService.list(source, rankingPeriod, rankingDate, format, commander, archetype, bracket, colorIdentity, limit)).build();
    }

    @GET
    @Path("/top-decks/{id}")
    public Response topDeck(@HeaderParam("X-Admin-Key") String adminKey, @PathParam("id") Long id) {
        if (!isTopDeckAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        var deck = metaTopDeckService.get(id);
        if (deck == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(deck).build();
    }

    @POST
    @Path("/top-decks/sync")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response syncTopDecks(
            @HeaderParam("X-Admin-Key") String adminKey,
            MetaTopDeckSyncRequestDTO request
    ) {
        if (!isTopDeckAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(metaTopDeckService.sync(request)).build();
    }

    @POST
    @Path("/combos/sync")
    public Response syncCombos(
            @HeaderParam("X-Admin-Key") String adminKey,
            @QueryParam("query") String query,
            @QueryParam("limit") Integer limit
    ) {
        if (!isTopDeckAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        int imported = comboSyncService.sync(query == null || query.isBlank() ? "legal:commander" : query, limit == null ? 500 : limit);
        return Response.ok(Map.of("source", "Commander Spellbook", "imported", imported)).build();
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
        return hasValidAdminKey(adminKey);
    }

    private boolean isTopDeckAdminAuthorized(String adminKey) {
        if (hasValidAdminKey(adminKey)) {
            return true;
        }
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            return false;
        }

        try {
            String email = authenticatedUserService.profile(securityIdentity).email();
            if (email == null || email.isBlank()) {
                return false;
            }
            return parseTopDeckAdminEmails().contains(email.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private Set<String> parseTopDeckAdminEmails() {
        return topDeckAdminEmails
                .map(value -> Arrays.stream(value.split(","))
                        .map(email -> email.trim().toLowerCase(Locale.ROOT))
                        .filter(email -> !email.isBlank())
                        .collect(Collectors.toUnmodifiableSet()))
                .orElse(Set.of());
    }

    private boolean hasValidAdminKey(String adminKey) {
        return syncApiKey.isPresent() && !syncApiKey.get().isBlank() && syncApiKey.get().equals(adminKey);
    }
}
