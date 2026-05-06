package com.mtg.service.meta;

import java.time.OffsetDateTime;
import java.util.List;

public record MetaSourceStatus(
        String name,
        boolean enabled,
        OffsetDateTime lastSync,
        List<String> supportedBrackets,
        String usage
) {
}
