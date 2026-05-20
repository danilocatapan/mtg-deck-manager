package com.mtg.controller;

import com.mtg.dto.ErrorResponseDTO;
import com.mtg.dto.SecurityStatusRequestDTO;
import com.mtg.dto.SecurityStatusResponseDTO;
import com.mtg.service.AuthenticatedUserService;
import com.mtg.service.SecurityStatusService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/security/status")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SecurityResource {
    private static final Logger LOG = Logger.getLogger(SecurityResource.class);
    private static final Set<String> ALLOWED_REQUEST_FIELDS = Set.of("includeDetails", "scanExternalDependencies");

    @Inject
    SecurityStatusService securityStatusService;

    @Inject
    AuthenticatedUserService authenticatedUserService;

    @Inject
    SecurityIdentity securityIdentity;

    @ConfigProperty(name = "security.admin.subjects")
    Optional<String> adminSubjects;

    @POST
    @Path("/check")
    @Authenticated
    @Operation(summary = "Check security posture", description = "Returns a redacted, read-only security posture summary for administrators.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Security status returned"),
            @APIResponse(responseCode = "401", description = "Authentication required"),
            @APIResponse(responseCode = "403", description = "Admin permission required")
    })
    public Response check(Map<String, Object> requestPayload) {
        String subject = authenticatedUserService.subject(securityIdentity);
        if (!isAdmin(subject)) {
            LOG.warnf("event=security.status.denied subject=%s", subject == null ? "unknown" : "present");
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponseDTO("Admin permission is required"))
                    .build();
        }

        LOG.info("event=security.status.check");
        SecurityStatusRequestDTO request = parseRequest(requestPayload);
        SecurityStatusResponseDTO response = securityStatusService.check(request);
        return Response.ok(response)
                .header("Cache-Control", "no-store")
                .build();
    }

    private boolean isAdmin(String subject) {
        if (securityIdentity != null && securityIdentity.hasRole("admin")) {
            return true;
        }
        if (subject == null || subject.isBlank()) {
            return false;
        }
        Set<String> allowedSubjects = parseAdminSubjects();
        return allowedSubjects.contains(subject.trim());
    }

    private Set<String> parseAdminSubjects() {
        return adminSubjects
                .map(value -> Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(subject -> !subject.isBlank())
                        .collect(Collectors.toUnmodifiableSet()))
                .orElse(Set.of());
    }

    private SecurityStatusRequestDTO parseRequest(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            LOG.debug("event=security.status.payload.empty");
            return new SecurityStatusRequestDTO(false, false);
        }
        LOG.debugf("event=security.status.payload.received fieldCount=%d", payload.size());
        for (String field : payload.keySet()) {
            if (!ALLOWED_REQUEST_FIELDS.contains(field)) {
                LOG.warnf("event=security.status.payload.invalid reason=unknown_field field=%s", field);
                throw new IllegalArgumentException("Unknown security status field: " + field);
            }
        }
        return new SecurityStatusRequestDTO(
                booleanField(payload, "includeDetails"),
                booleanField(payload, "scanExternalDependencies")
        );
    }

    private Boolean booleanField(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        LOG.warnf("event=security.status.payload.invalid reason=invalid_boolean field=%s", field);
        throw new IllegalArgumentException(field + " must be a boolean");
    }
}
