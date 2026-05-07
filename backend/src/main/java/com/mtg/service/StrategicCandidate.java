package com.mtg.service;

import com.mtg.dto.CardResponseDTO;

record StrategicCandidate(
        CardResponseDTO card,
        String role,
        double score,
        String reason,
        boolean metaDriven,
        double inclusionRate
) {
    StrategicCandidate(CardResponseDTO card, String role, double score, String reason) {
        this(card, role, score, reason, false, 0.0);
    }
}
