package com.mtg.domain;

import java.util.List;

public record ComboNearMiss(
        String name,
        List<String> presentCards,
        String missingCard,
        String result,
        String source
) {
    public ComboNearMiss {
        presentCards = presentCards == null ? List.of() : List.copyOf(presentCards);
    }
}
