package com.mtg.domain;

import java.util.List;

public record ComboAnalysis(
        List<ComboHit> present,
        List<ComboNearMiss> oneCardAway,
        String source,
        String version,
        String updatedAt
) {
    public ComboAnalysis {
        present = present == null ? List.of() : List.copyOf(present);
        oneCardAway = oneCardAway == null ? List.of() : List.copyOf(oneCardAway);
        source = source == null || source.isBlank() ? "local_snapshot" : source;
        version = version == null || version.isBlank() ? "unknown" : version;
        updatedAt = updatedAt == null || updatedAt.isBlank() ? "unknown" : updatedAt;
    }

    public ComboAnalysis(List<ComboHit> present, List<ComboNearMiss> oneCardAway) {
        this(present, oneCardAway, "local_snapshot", "unknown", "unknown");
    }

    public static ComboAnalysis empty() {
        return new ComboAnalysis(List.of(), List.of(), "local_snapshot", "unknown", "unknown");
    }
}
