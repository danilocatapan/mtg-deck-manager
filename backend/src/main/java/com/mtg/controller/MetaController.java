package com.mtg.controller;

import com.mtg.dto.ExternalDeckImportRequestDTO;
import com.mtg.dto.ExternalDeckImportResponseDTO;
import com.mtg.dto.RecommendationBenchmarkReviewRequestDTO;
import com.mtg.service.AuthenticatedUserService;
import com.mtg.service.CommanderSpellbookComboSyncService;
import com.mtg.service.ExternalDeckImportService;
import com.mtg.service.RecommendationBenchmarkService;
import com.mtg.service.RecommendationBenchmarkAiService;
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
    CommanderSpellbookComboSyncService comboSyncService;

    @Inject
    RecommendationBenchmarkService recommendationBenchmarkService;

    @Inject
    RecommendationBenchmarkAiService recommendationBenchmarkAiService;

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
        if (!isTopDeckAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(ingestionJob.sync()).build();
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
    @Path("/recommendation-benchmark/summary")
    public Response recommendationBenchmarkSummary(@HeaderParam("X-Admin-Key") String adminKey) {
        if (!isTopDeckAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(recommendationBenchmarkService.summary()).build();
    }

    @POST
    @Path("/recommendation-benchmark/run")
    public Response runRecommendationBenchmark(@HeaderParam("X-Admin-Key") String adminKey) {
        if (!isTopDeckAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            return Response.ok(recommendationBenchmarkService.run()).build();
        } catch (IllegalStateException exception) {
            if ("benchmark_already_running".equals(exception.getMessage())) {
                return Response.status(Response.Status.CONFLICT).entity(Map.of("code", "benchmark_already_running")).build();
            }
            throw exception;
        }
    }

    @GET
    @Path("/recommendation-benchmark/reviews/next")
    public Response nextRecommendationBenchmarkReview(@HeaderParam("X-Admin-Key") String adminKey) {
        if (!isTopDeckAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        var review = recommendationBenchmarkService.nextReview(currentAdminId());
        return review == null ? Response.noContent().build() : Response.ok(review).build();
    }

    @POST
    @Path("/recommendation-benchmark/reviews/{caseId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reviewRecommendationBenchmark(
            @HeaderParam("X-Admin-Key") String adminKey,
            @PathParam("caseId") String caseId,
            RecommendationBenchmarkReviewRequestDTO request
    ) {
        if (!isTopDeckAdminAuthorized(adminKey)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        validateLength(caseId, 120, "caseId");
        recommendationBenchmarkService.review(caseId, request, currentAdminId());
        return Response.noContent().build();
    }

    @POST
    @Path("/recommendation-benchmark/ai-artifacts/preview")
    public Response recommendationBenchmarkAiPreview(@HeaderParam("X-Admin-Key") String adminKey) {
        if (!isTopDeckAdminAuthorized(adminKey)) return Response.status(Response.Status.FORBIDDEN).build();
        return Response.ok(recommendationBenchmarkAiService.preview()).build();
    }

    @POST
    @Path("/recommendation-benchmark/ai-artifacts/generate")
    public Response generateRecommendationBenchmarkAiArtifacts(@HeaderParam("X-Admin-Key") String adminKey) {
        if (!isTopDeckAdminAuthorized(adminKey)) return Response.status(Response.Status.FORBIDDEN).build();
        try {
            return Response.accepted(recommendationBenchmarkAiService.start()).build();
        } catch (IllegalStateException exception) {
            if ("openai_not_configured".equals(exception.getMessage())) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(Map.of("code", "openai_not_configured")).build();
            }
            if ("corpus_not_ready".equals(exception.getMessage())) {
                return Response.status(Response.Status.CONFLICT).entity(Map.of("code", "corpus_not_ready")).build();
            }
            if ("ai_artifact_job_already_running".equals(exception.getMessage())) {
                return Response.status(Response.Status.CONFLICT).entity(Map.of("code", "ai_artifact_job_already_running")).build();
            }
            throw exception;
        }
    }

    @GET
    @Path("/recommendation-benchmark/ai-artifacts/jobs/{id}")
    public Response recommendationBenchmarkAiJob(@HeaderParam("X-Admin-Key") String adminKey, @PathParam("id") Long id) {
        if (!isTopDeckAdminAuthorized(adminKey)) return Response.status(Response.Status.FORBIDDEN).build();
        return Response.ok(recommendationBenchmarkAiService.job(id)).build();
    }

    @GET
    @Path("/recommendation-benchmark/cases/{caseId}/comparison")
    public Response recommendationBenchmarkComparison(@HeaderParam("X-Admin-Key") String adminKey, @PathParam("caseId") String caseId) {
        if (!isTopDeckAdminAuthorized(adminKey)) return Response.status(Response.Status.FORBIDDEN).build();
        validateLength(caseId, 120, "caseId");
        return Response.ok(recommendationBenchmarkAiService.comparison(caseId)).build();
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

    private String currentAdminId() {
        if (securityIdentity != null && !securityIdentity.isAnonymous()) {
            return securityIdentity.getPrincipal().getName();
        }
        return "admin-key";
    }
}
