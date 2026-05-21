package com.mtg.dto;

import java.time.LocalDate;

public record MetaTopDeckSyncResponseDTO(
        Long batchId,
        String status,
        String message,
        String source,
        String rankingPeriod,
        LocalDate rankingDate,
        String profileRefreshStatus
) {
}
