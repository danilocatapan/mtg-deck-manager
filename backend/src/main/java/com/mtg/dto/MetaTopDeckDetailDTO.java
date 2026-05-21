package com.mtg.dto;

import java.time.LocalDate;
import java.util.List;

public record MetaTopDeckDetailDTO(
        Long id,
        String source,
        String sourceUrl,
        String deckUrl,
        String rankingPeriod,
        LocalDate rankingDate,
        String format,
        int rank,
        String name,
        String commander,
        String archetype,
        String bracket,
        List<String> colorIdentity,
        Integer wins,
        Integer losses,
        Double popularityScore,
        List<MetaTopDeckCardResponseDTO> cards
) {
}
