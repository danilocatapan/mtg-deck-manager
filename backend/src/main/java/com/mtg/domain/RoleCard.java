package com.mtg.domain;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RoleCard")
public record RoleCard(
        String name,
        int quantity,
        String imageUrl
) {
}
