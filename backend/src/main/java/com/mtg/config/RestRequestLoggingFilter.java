package com.mtg.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.UUID;

@Provider
public class RestRequestLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOG = Logger.getLogger(RestRequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String requestId = resolveRequestId(requestContext);
        String method = requestContext.getMethod();
        String endpoint = resolveEndpoint(requestContext);

        RequestLogContext.set(new RequestLogContext.RequestInfo(
                requestId,
                method,
                endpoint,
                System.currentTimeMillis()
        ));

        StructuredRestLog.requestReceived(
                LOG,
                requestId,
                method,
                endpoint,
                requestContext.getHeaderString("Content-Type"),
                resolveClientIp(requestContext)
        );
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        RequestLogContext.RequestInfo info = RequestLogContext.get();
        if (info == null) {
            return;
        }

        responseContext.getHeaders().putSingle(REQUEST_ID_HEADER, info.requestId());
        long durationMs = Math.max(0, System.currentTimeMillis() - info.startedAtMillis());
        StructuredRestLog.requestCompleted(
                LOG,
                info.requestId(),
                info.method(),
                info.endpoint(),
                responseContext.getStatus(),
                durationMs
        );
        RequestLogContext.clear();
    }

    private String resolveRequestId(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }

    private String resolveEndpoint(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath(true);
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private String resolveClientIp(ContainerRequestContext requestContext) {
        String forwardedFor = requestContext.getHeaderString("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = requestContext.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return "unknown";
    }
}
