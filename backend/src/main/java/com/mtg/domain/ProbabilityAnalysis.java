package com.mtg.domain;

public record ProbabilityAnalysis(
        double openingHandTwoPlusLands,
        double rampByTurnTwo,
        double interactionByTurnThree
) {
    public static ProbabilityAnalysis empty() {
        return new ProbabilityAnalysis(0.0, 0.0, 0.0);
    }
}
