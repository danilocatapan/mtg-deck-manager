package com.mtg.domain;

import java.util.Map;

public record ManaBaseAnalysis(
        Map<String, Integer> colorCosts,
        Map<String, Integer> colorSources,
        int landCount,
        int tappedLandCount,
        Map<Integer, Integer> untappedSourcesByTurn
) {
    public ManaBaseAnalysis {
        colorCosts = colorCosts == null ? Map.of() : Map.copyOf(colorCosts);
        colorSources = colorSources == null ? Map.of() : Map.copyOf(colorSources);
        untappedSourcesByTurn = untappedSourcesByTurn == null ? Map.of() : Map.copyOf(untappedSourcesByTurn);
    }

    public static ManaBaseAnalysis empty() {
        return new ManaBaseAnalysis(Map.of(), Map.of(), 0, 0, Map.of());
    }
}
