package com.mtg.domain;

import java.util.Map;

public record ManaBaseAnalysis(
        Map<String, Integer> colorCosts,
        Map<String, Integer> colorSources,
        int landCount,
        int tappedLandCount,
        Map<Integer, Integer> untappedSourcesByTurn,
        Map<String, Integer> pipDemand,
        int fixingSourceCount,
        int treasureSourceCount,
        int manaRockCount,
        int fetchLandCount,
        int conditionalSourceCount
) {
    public ManaBaseAnalysis {
        colorCosts = colorCosts == null ? Map.of() : Map.copyOf(colorCosts);
        colorSources = colorSources == null ? Map.of() : Map.copyOf(colorSources);
        untappedSourcesByTurn = untappedSourcesByTurn == null ? Map.of() : Map.copyOf(untappedSourcesByTurn);
        pipDemand = pipDemand == null ? Map.of() : Map.copyOf(pipDemand);
    }

    public ManaBaseAnalysis(
            Map<String, Integer> colorCosts,
            Map<String, Integer> colorSources,
            int landCount,
            int tappedLandCount,
            Map<Integer, Integer> untappedSourcesByTurn
    ) {
        this(colorCosts, colorSources, landCount, tappedLandCount, untappedSourcesByTurn, Map.of(), 0, 0, 0, 0, 0);
    }

    public static ManaBaseAnalysis empty() {
        return new ManaBaseAnalysis(Map.of(), Map.of(), 0, 0, Map.of(), Map.of(), 0, 0, 0, 0, 0);
    }
}
