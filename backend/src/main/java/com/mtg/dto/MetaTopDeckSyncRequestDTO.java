package com.mtg.dto;

import java.time.LocalDate;
import java.util.List;

public record MetaTopDeckSyncRequestDTO(
        String source,
        String rankingPeriod,
        LocalDate rankingDate,
        Integer limitPerGroup,
        String groupBy,
        List<String> commanders,
        List<String> archetypes,
        List<String> brackets
) {
}
