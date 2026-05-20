package com.mtg.service;

import com.mtg.dto.SecurityIssueDTO;
import com.mtg.dto.SecurityStatusRequestDTO;
import com.mtg.dto.SecurityStatusResponseDTO;
import io.smallrye.config.SmallRyeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class SecurityStatusService {
    private static final Logger LOG = Logger.getLogger(SecurityStatusService.class);

    @ConfigProperty(name = "app.environment", defaultValue = "local")
    String environment;

    public SecurityStatusResponseDTO check(SecurityStatusRequestDTO request) {
        SecurityStatusRequestDTO safeRequest = request == null
                ? new SecurityStatusRequestDTO(false, false)
                : request;
        List<SecurityIssueDTO> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        Map<String, Object> details = new LinkedHashMap<>();
        boolean production = isProduction();
        LOG.infof(
                "event=security.status.evaluate.start includeDetails=%s scanExternalDependencies=%s production=%s",
                safeRequest.includeDetailsEnabled(),
                safeRequest.scanExternalDependenciesEnabled(),
                production
        );

        evaluateCors(issues, recommendations, details, safeRequest.includeDetailsEnabled(), production);
        evaluateOidc(issues, recommendations, details, safeRequest.includeDetailsEnabled(), production);
        evaluateHttpSecurity(issues, recommendations, details, safeRequest.includeDetailsEnabled());
        evaluateDependencyScanMode(issues, recommendations, details, safeRequest.scanExternalDependenciesEnabled());

        recommendations.add("Keep OAuth/OIDC scopes limited to openid, email and profile.");
        recommendations.add("Keep security diagnostics read-only and redact all secrets, tokens and personal data.");
        recommendations.add("Validate OWASP API Security Top 10, REST security headers and NIST session guidance during release hardening.");

        String status = statusFor(issues);
        LOG.infof(
                "event=security.status.evaluate.finish status=%s issueCount=%d recommendationCount=%d detailKeys=%d",
                status,
                issues.size(),
                recommendations.size(),
                details.size()
        );

        return new SecurityStatusResponseDTO(
                status,
                safeEnvironment(production),
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                List.copyOf(issues),
                List.copyOf(recommendations),
                Map.copyOf(details)
        );
    }

    private void evaluateCors(
            List<SecurityIssueDTO> issues,
            List<String> recommendations,
            Map<String, Object> details,
            boolean includeDetails,
            boolean production
    ) {
        Optional<String> origins = configValue("quarkus.http.cors.origins");
        Optional<Boolean> credentials = configValue("quarkus.http.cors.access-control-allow-credentials", Boolean.class);
        LOG.debugf(
                "event=security.status.cors.evaluate originsConfigured=%s credentials=%s production=%s",
                origins.isPresent() && !origins.get().isBlank(),
                credentials.orElse(false),
                production
        );

        if (origins.isEmpty() || origins.get().isBlank()) {
            issues.add(issue(
                    "CORS_POLICY",
                    "MEDIUM",
                    "CORS allowed origins are not explicitly configured.",
                    "Configure a small allowlist for frontend origins."
            ));
        } else if (origins.get().contains("*")) {
            issues.add(issue(
                    "CORS_POLICY",
                    production ? "HIGH" : "MEDIUM",
                    "CORS allows wildcard origins.",
                    "Replace wildcard origins with explicit frontend domains before production deployment."
            ));
        } else if (production && origins.get().contains("localhost")) {
            issues.add(issue(
                    "CORS_POLICY",
                    "MEDIUM",
                    "Production CORS configuration still includes localhost.",
                    "Keep localhost origins in dev/test profiles only."
            ));
        }

        if (Boolean.TRUE.equals(credentials.orElse(false)) && origins.orElse("").contains("*")) {
            issues.add(issue(
                    "CORS_CREDENTIALS",
                    "HIGH",
                    "CORS credentials are enabled with broad origins.",
                    "Disable credentials or use a strict origin allowlist."
            ));
        }

        recommendations.add("Restrict CORS origins, methods and headers to the deployed frontend needs.");

        if (includeDetails) {
            details.put("cors", Map.of(
                    "enabled", configValue("quarkus.http.cors.enabled").orElse("unknown"),
                    "origins", production ? "redacted-in-production" : origins.orElse("not-configured"),
                    "credentials", credentials.orElse(false)
            ));
        }
    }

    private void evaluateOidc(
            List<SecurityIssueDTO> issues,
            List<String> recommendations,
            Map<String, Object> details,
            boolean includeDetails,
            boolean production
    ) {
        Optional<String> authServer = configValue("quarkus.oidc.auth-server-url");
        Optional<String> clientId = configValue("quarkus.oidc.client-id");
        Optional<String> adminSubjects = configValue("security.admin.subjects");
        LOG.debugf(
                "event=security.status.auth.evaluate providerConfigured=%s clientIdConfigured=%s adminSubjectsConfigured=%s",
                authServer.isPresent() && !authServer.get().isBlank(),
                clientId.isPresent() && !clientId.get().isBlank(),
                adminSubjects.isPresent() && !adminSubjects.get().isBlank()
        );

        if (authServer.isEmpty() || authServer.get().isBlank()) {
            issues.add(issue(
                    "OIDC_PROVIDER",
                    "HIGH",
                    "OIDC auth-server-url is not configured.",
                    "Configure a trusted OIDC provider for authenticated operations."
            ));
        }

        if (clientId.isEmpty() || clientId.get().isBlank()) {
            issues.add(issue(
                    "OIDC_CLIENT_ID",
                    "HIGH",
                    "OIDC client id is not configured.",
                    "Set GOOGLE_CLIENT_ID for token audience validation."
            ));
        }

        if (adminSubjects.isEmpty() || adminSubjects.get().isBlank()) {
            issues.add(issue(
                    "ADMIN_AUTHORIZATION",
                    "HIGH",
                    "No admin subject allowlist is configured for security diagnostics.",
                    "Set SECURITY_ADMIN_SUBJECTS with explicit Google subject identifiers for administrators."
            ));
        }

        recommendations.add("Use explicit admin subject allowlists or provider roles for administrative endpoints.");

        if (includeDetails) {
            details.put("auth", Map.of(
                    "providerConfigured", authServer.isPresent() && !authServer.get().isBlank(),
                    "clientIdConfigured", clientId.isPresent() && !clientId.get().isBlank(),
                    "adminSubjectsConfigured", adminSubjects.isPresent() && !adminSubjects.get().isBlank(),
                    "sensitiveValues", production ? "redacted-in-production" : "redacted"
            ));
        }
    }

    private void evaluateHttpSecurity(
            List<SecurityIssueDTO> issues,
            List<String> recommendations,
            Map<String, Object> details,
            boolean includeDetails
    ) {
        recommendations.add("Serve browser-facing assets with CSP, frame restrictions, nosniff, referrer and permissions policies.");
        recommendations.add("Prefer TLS 1.3 and HSTS at the reverse proxy or hosting layer.");

        if (includeDetails) {
            LOG.debug("event=security.status.headers.details_included");
            details.put("httpHeaders", Map.of(
                    "xContentTypeOptions", "nosniff",
                    "xFrameOptions", "DENY",
                    "referrerPolicy", "strict-origin-when-cross-origin",
                    "permissionsPolicy", "camera=(), microphone=(), geolocation=()",
                    "contentSecurityPolicy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"
            ));
        }
    }

    private void evaluateDependencyScanMode(
            List<SecurityIssueDTO> issues,
            List<String> recommendations,
            Map<String, Object> details,
            boolean scanExternalDependencies
    ) {
        LOG.debugf("event=security.status.dependencies.evaluate requested=%s runtimeExternalCalls=false", scanExternalDependencies);
        if (scanExternalDependencies) {
            issues.add(issue(
                    "DEPENDENCY_SCAN_RUNTIME",
                    "LOW",
                    "External dependency scanning was requested at runtime.",
                    "Use CI-generated SBOM/dependency scan results instead of network-dependent scans in the request path."
            ));
        }

        recommendations.add("Run dependency, SBOM and vulnerability checks in CI/CD and surface only summarized results here.");

        if (scanExternalDependencies) {
            details.put("dependencyScan", Map.of(
                    "runtimeExternalCalls", false,
                    "mode", "summary-only"
            ));
        }
    }

    private SecurityIssueDTO issue(String type, String severity, String description, String recommendation) {
        return new SecurityIssueDTO(type, severity, description, recommendation);
    }

    private String statusFor(List<SecurityIssueDTO> issues) {
        boolean hasHigh = issues.stream().anyMatch(issue -> "HIGH".equals(issue.severity()));
        if (hasHigh) {
            return "warning";
        }
        return issues.isEmpty() ? "ok" : "warning";
    }

    private boolean isProduction() {
        String normalized = environment == null ? "" : environment.trim().toLowerCase(Locale.ROOT);
        return "prod".equals(normalized) || "production".equals(normalized);
    }

    private String safeEnvironment(boolean production) {
        return production ? "production" : (environment == null || environment.isBlank() ? "local" : environment);
    }

    private Optional<String> configValue(String name) {
        return configValue(name, String.class);
    }

    private <T> Optional<T> configValue(String name, Class<T> type) {
        Config config = ConfigProvider.getConfig();
        try {
            SmallRyeConfig smallRyeConfig = config.unwrap(SmallRyeConfig.class);
            return smallRyeConfig.getOptionalValue(name, type);
        } catch (IllegalArgumentException ignored) {
            return config.getOptionalValue(name, type);
        }
    }
}
