package com.mtg.service.rules;

import java.util.HashMap;
import java.util.Map;

public final class HeuristicRules {

    private HeuristicRules() {}

    public static Map<String, Integer> calculateGaps(int rampCount, int drawCount, int removalCount, double averageCmc) {
        return calculateGaps(rampCount, drawCount, removalCount, averageCmc, "casual");
    }

    public static Map<String, Integer> calculateGaps(int rampCount, int drawCount, int removalCount, double averageCmc, String bracket) {
        Map<String, Integer> gaps = new HashMap<>();

        int rampThreshold = "high".equalsIgnoreCase(bracket) ? 12 : 10;
        int drawThreshold = "high".equalsIgnoreCase(bracket) ? 10 : 8;
        int removalThreshold = "high".equalsIgnoreCase(bracket) ? 10 : 8;

        if (rampCount < rampThreshold) gaps.put("ramp", rampThreshold - rampCount);
        if (drawCount < drawThreshold) gaps.put("draw", drawThreshold - drawCount);
        if (removalCount < removalThreshold) gaps.put("removal", removalThreshold - removalCount);

        return gaps;
    }

    public static boolean prioritizeLowCmc(double averageCmc) {
        return averageCmc > 3.5;
    }
}
