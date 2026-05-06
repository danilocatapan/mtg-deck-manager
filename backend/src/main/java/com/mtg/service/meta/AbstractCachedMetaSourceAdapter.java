package com.mtg.service.meta;

import java.time.OffsetDateTime;
import java.util.List;

abstract class AbstractCachedMetaSourceAdapter implements MetaSourceAdapter {
    @Override
    public List<MetaDeck> fetchDecks(String bracket) {
        return List.of();
    }

    @Override
    public MetaSourceStatus status() {
        return new MetaSourceStatus(sourceName(), true, OffsetDateTime.now(), supportedBrackets(), "offline-cache");
    }
}
