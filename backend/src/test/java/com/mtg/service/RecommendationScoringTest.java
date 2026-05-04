package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationScoringTest {

    @Test
    void lowCmcBonusApplied() {
        CardResponseDTO low = new CardResponseDTO("Cheap","{1}","Instant","Draw a card.",1.0);
        CardResponseDTO high = new CardResponseDTO("Expensive","{6}","Creature","Big effect.",6.0);

        double sLow = RecommendationScoring.score(low, "draw");
        double sHigh = RecommendationScoring.score(high, "draw");

        assertTrue(sLow > sHigh);
    }
}
