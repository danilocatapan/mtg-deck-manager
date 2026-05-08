package com.mtg.dto;

import java.util.List;

public record RulesSnapshotDTO(
        String banlistDate,
        String gameChangersDate,
        String bracketVersion,
        String scryfallLegalityVersion,
        List<String> checks
) {
    public RulesSnapshotDTO {
        checks = checks == null ? List.of() : List.copyOf(checks);
    }
}
