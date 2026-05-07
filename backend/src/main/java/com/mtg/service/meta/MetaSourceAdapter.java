package com.mtg.service.meta;

import java.util.List;

public interface MetaSourceAdapter {
    String sourceName();
    List<String> supportedBrackets();
    List<MetaDeck> fetchDecks(String bracket);
    MetaSourceStatus status();

    default List<MetaDeck> sync() {
        return fetchDecks(null);
    }
}
