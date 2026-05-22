package com.mtg.dto;

import java.time.LocalDate;
import java.util.List;

public record MetaTopDeckImportRequestDTO(
        String source,
        String sourceUrl,
        String rankingPeriod,
        LocalDate rankingDate,
        String format,
        String importFormat,
        String decklistFormat,
        List<MetaTopDeckImportDeckDTO> decks
) {
}
