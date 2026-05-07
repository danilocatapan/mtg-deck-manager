package com.mtg.dto;

import java.util.List;

public record BanlistStatusDTO(
        boolean legal,
        List<String> bannedCards
) {
    public BanlistStatusDTO {
        bannedCards = bannedCards == null ? List.of() : List.copyOf(bannedCards);
    }
}
