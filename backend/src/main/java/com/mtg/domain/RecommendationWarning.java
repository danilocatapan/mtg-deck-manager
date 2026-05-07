package com.mtg.domain;

public record RecommendationWarning(
        String category,
        String severity,
        String message,
        String rule,
        String suggestedAction
) {
}
