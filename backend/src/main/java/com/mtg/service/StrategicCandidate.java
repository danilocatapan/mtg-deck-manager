package com.mtg.service;

import com.mtg.dto.CardResponseDTO;

record StrategicCandidate(
        CardResponseDTO card,
        String role,
        double score,
        String reason
) {
}
