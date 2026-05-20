package com.mtg.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        responseContext.getHeaders().putSingle("X-Content-Type-Options", "nosniff");
        responseContext.getHeaders().putSingle("X-Frame-Options", "DENY");
        responseContext.getHeaders().putSingle("Referrer-Policy", "strict-origin-when-cross-origin");
        responseContext.getHeaders().putSingle("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        responseContext.getHeaders().putSingle(
                "Content-Security-Policy",
                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"
        );

        if (requestContext.getUriInfo().getRequestUri().getScheme().equalsIgnoreCase("https")) {
            responseContext.getHeaders().putSingle("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
    }
}
