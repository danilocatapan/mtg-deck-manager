package com.mtg.service;

import com.mtg.dto.CardResponseDTO;

public final class RecommendationScoring {

    private RecommendationScoring() {}

    public static double score(CardResponseDTO card, String role) {
        double score = 0.0;

        // base weight by role
        switch (role) {
            case "ramp" -> score += 1.0;
            case "draw" -> score += 0.9;
            case "removal" -> score += 0.95;
            default -> score += 0.5;
        }

        Double cmc = card.cmc();
        if (cmc != null) {
            if (cmc <= 2.0) score += 0.5;
            else if (cmc == 3.0) score += 0.2;
        }

        String text = card.oracleText();
        if (text != null && text.length() < 80) {
            score += 0.3;
        }

        return score;
    }
}
