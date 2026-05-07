package com.mtg.domain;

import java.util.List;

public record ComboAnalysis(
        List<ComboHit> present,
        List<ComboNearMiss> oneCardAway
) {
    public ComboAnalysis {
        present = present == null ? List.of() : List.copyOf(present);
        oneCardAway = oneCardAway == null ? List.of() : List.copyOf(oneCardAway);
    }

    public static ComboAnalysis empty() {
        return new ComboAnalysis(List.of(), List.of());
    }
}
