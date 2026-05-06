package com.mtg.service.meta;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record MetaDeck(
        String source,
        String externalId,
        String commander,
        List<String> partnerCommanders,
        List<String> colorIdentity,
        String bracket,
        List<String> archetypes,
        List<MetaDeckCard> cards,
        String eventName,
        LocalDate eventDate,
        Integer placement,
        Integer playerCount,
        String url,
        OffsetDateTime fetchedAt
) {
    public MetaDeck {
        partnerCommanders = partnerCommanders == null ? List.of() : List.copyOf(partnerCommanders);
        colorIdentity = colorIdentity == null ? List.of() : List.copyOf(colorIdentity);
        archetypes = archetypes == null ? List.of() : List.copyOf(archetypes);
        cards = cards == null ? List.of() : List.copyOf(cards);
        fetchedAt = fetchedAt == null ? OffsetDateTime.now() : fetchedAt;
    }
}
