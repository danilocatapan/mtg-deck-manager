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

        String normalized = bracket == null ? "casual" : bracket.trim().toLowerCase().replace("_", "-");
        boolean highPower = normalized.equals("4") || normalized.equals("5")
                || normalized.equals("high")
                || normalized.equals("high-power")
                || normalized.equals("highpower")
                || normalized.equals("cedh");

        int rampThreshold = highPower ? 12 : 10;
        int drawThreshold = highPower ? 10 : 8;
        int removalThreshold = highPower ? 10 : 8;

        if (rampCount < rampThreshold) gaps.put("ramp", rampThreshold - rampCount);
        if (drawCount < drawThreshold) gaps.put("draw", drawThreshold - drawCount);
        if (removalCount < removalThreshold) gaps.put("removal", removalThreshold - removalCount);

        return gaps;
    }

    public static boolean prioritizeLowCmc(double averageCmc) {
        return averageCmc > 3.5;
    }
}
