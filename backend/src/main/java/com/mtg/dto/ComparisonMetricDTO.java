package com.mtg.dto;

public record ComparisonMetricDTO(
        String key,
        String label,
        double deckValue,
        double similarAverage,
        String status,
        String message
) {
}
