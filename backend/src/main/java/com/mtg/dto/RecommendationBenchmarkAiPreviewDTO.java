package com.mtg.dto;

public record RecommendationBenchmarkAiPreviewDTO(
        boolean configured,
        String model,
        String fixtureVersion,
        String algorithmVersion,
        String promptVersion,
        int totalCases,
        int baselineCalls,
        int judgeCalls,
        int totalCalls,
        int maxConcurrency,
        String status
) {}
