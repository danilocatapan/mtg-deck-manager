package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.domain.CutScoreBreakdown;
import com.mtg.domain.RecommendationScoreBreakdown;

record StrategicCandidate(
        CardResponseDTO card,
        String role,
        double score,
        String reason,
        boolean metaDriven,
        double inclusionRate,
        double synergyEstimate,
        String source,
        RecommendationScoreBreakdown addScoreBreakdown,
        CutScoreBreakdown cutScoreBreakdown
) {
    StrategicCandidate(
            CardResponseDTO card,
            String role,
            double score,
            String reason,
            boolean metaDriven,
            double inclusionRate,
            double synergyEstimate,
            String source
    ) {
        this(card, role, score, reason, metaDriven, inclusionRate, synergyEstimate, source, null, null);
    }

    StrategicCandidate(CardResponseDTO card, String role, double score, String reason) {
        this(card, role, score, reason, false, 0.0, 0.0, "heuristic_fallback");
    }

    StrategicCandidate(CardResponseDTO card, String role, double score, String reason, boolean metaDriven, double inclusionRate) {
        this(card, role, score, reason, metaDriven, inclusionRate, 0.0, metaDriven ? "meta_profile" : "heuristic_fallback");
    }
}
