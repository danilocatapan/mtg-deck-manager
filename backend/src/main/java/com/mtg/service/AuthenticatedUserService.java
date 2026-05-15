package com.mtg.service;

import com.mtg.dto.AuthenticatedUserDTO;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.security.Principal;

@ApplicationScoped
public class AuthenticatedUserService {

    public AuthenticatedUserDTO profile(SecurityIdentity identity) {
        String subject = firstNonBlank(claim(identity, "sub"), principalName(identity));
        return new AuthenticatedUserDTO(
                subject,
                claim(identity, "email"),
                claim(identity, "name"),
                claim(identity, "picture")
        );
    }

    public String subject(SecurityIdentity identity) {
        String subject = firstNonBlank(claim(identity, "sub"), principalName(identity));
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return subject;
    }

    private String claim(SecurityIdentity identity, String name) {
        if (identity == null || name == null || name.isBlank()) {
            return null;
        }

        Object attribute = identity.getAttribute(name);
        if (attribute instanceof String value && !value.isBlank()) {
            return value;
        }

        Principal principal = identity.getPrincipal();
        if (principal instanceof JsonWebToken token) {
            Object claim = token.getClaim(name);
            if (claim instanceof String value && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String principalName(SecurityIdentity identity) {
        if (identity == null || identity.getPrincipal() == null) {
            return null;
        }
        String name = identity.getPrincipal().getName();
        return name == null || name.isBlank() ? null : name;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }
}
