package com.mtg.dto;

import java.time.OffsetDateTime;

public record RecommendationBenchmarkAiJobDTO(
        Long id,
        String status,
        String model,
        int totalCalls,
        int completedCalls,
        int failedCalls,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorCode,
        boolean promoted
) {}
