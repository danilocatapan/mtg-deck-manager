package com.mtg.service;

import com.mtg.dto.CardResponseDTO;

public final class RecommendationScoring {

    private RecommendationScoring() {}

    public static double score(CardResponseDTO card, String role) {
        return score(card, role, 0.0, 0.0);
    }

    public static double score(CardResponseDTO card, String role, double popularity, double synergy) {
        double score = 0.0;

        // base weight by role
        switch (role) {
            case "ramp" -> score += 1.0;
            case "draw" -> score += 0.9;
            case "removal" -> score += 0.95;
            default -> score += 0.5;
        }

        // popularity and synergy (meta has dominant weight)
        score += popularity * 0.6;
        score += synergy * 0.2;

        // efficiency
        Double cmc = card.cmc();
        double efficiency = 0.0;
        if (cmc != null) {
            if (cmc <= 2.0) efficiency = 0.5;
            else if (cmc == 3.0) efficiency = 0.2;
        }
        score += efficiency * 0.2;

        // penalty for very high cmc
        if (cmc != null && cmc >= 6.0) score -= 0.1;

        String text = card.oracleText();
        if (text != null && text.length() < 80) {
            score += 0.3;
        }

        return score;
    }
}
