package com.mtg.service.rules;

import java.util.HashMap;
import java.util.Map;

public final class HeuristicRules {

    private HeuristicRules() {}

    public static Map<String, Integer> calculateGaps(int rampCount, int drawCount, int removalCount, double averageCmc) {
        Map<String, Integer> gaps = new HashMap<>();

        if (rampCount < 10) gaps.put("ramp", 10 - rampCount);
        if (drawCount < 8) gaps.put("draw", 8 - drawCount);
        if (removalCount < 8) gaps.put("removal", 8 - removalCount);

        return gaps;
    }

    public static boolean prioritizeLowCmc(double averageCmc) {
        return averageCmc > 3.5;
    }
}
