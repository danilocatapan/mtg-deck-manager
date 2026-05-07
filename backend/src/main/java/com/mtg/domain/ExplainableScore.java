package com.mtg.domain;

public record ExplainableScore(
        int speed,
        int consistency,
        int interaction,
        int resilience,
        int threat,
        int bracketPressure,
        String summary
) {
    public static ExplainableScore empty() {
        return new ExplainableScore(0, 0, 0, 0, 0, 0, "");
    }
}
