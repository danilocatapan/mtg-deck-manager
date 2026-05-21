package com.mtg.dto;

import java.time.LocalDate;
import java.util.List;

public record MetaTopDeckSummaryDTO(
        Long id,
        String source,
        String rankingPeriod,
        LocalDate rankingDate,
        String format,
        int rank,
        String name,
        String commander,
        String deckUrl,
        String archetype,
        String bracket,
        List<String> colorIdentity,
        int cardsCount,
        Double popularityScore
) {
}
