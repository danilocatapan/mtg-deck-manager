package com.mtg.service;

import com.mtg.dto.CardResponseDTO;

record StrategicCandidate(
        CardResponseDTO card,
        String role,
        double score,
        String reason,
        boolean metaDriven,
        double inclusionRate,
        double synergyEstimate,
        String source
) {
    StrategicCandidate(CardResponseDTO card, String role, double score, String reason) {
        this(card, role, score, reason, false, 0.0, 0.0, "heuristic_fallback");
    }

    StrategicCandidate(CardResponseDTO card, String role, double score, String reason, boolean metaDriven, double inclusionRate) {
        this(card, role, score, reason, metaDriven, inclusionRate, 0.0, metaDriven ? "meta_profile" : "heuristic_fallback");
    }
}
