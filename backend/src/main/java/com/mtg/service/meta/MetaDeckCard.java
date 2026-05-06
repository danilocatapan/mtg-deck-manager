package com.mtg.service.meta;

import java.util.List;

public record MetaDeckCard(
        String name,
        int quantity,
        List<String> roles,
        Double cmc,
        String typeLine,
        List<String> colorIdentity
) {
    public MetaDeckCard {
        roles = roles == null ? List.of() : List.copyOf(roles);
        colorIdentity = colorIdentity == null ? List.of() : List.copyOf(colorIdentity);
    }
}
