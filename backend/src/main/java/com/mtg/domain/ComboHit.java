package com.mtg.domain;

import java.util.List;

public record ComboHit(
        String name,
        List<String> cards,
        String result,
        String source
) {
    public ComboHit {
        cards = cards == null ? List.of() : List.copyOf(cards);
    }
}
