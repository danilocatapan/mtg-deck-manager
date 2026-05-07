package com.mtg.domain;

public record RecommendationImpact(
        String role,
        Double averageCmcBefore,
        Double averageCmcAfter,
        Integer rampBefore,
        Integer rampAfter,
        Integer drawBefore,
        Integer drawAfter,
        Integer removalBefore,
        Integer removalAfter,
        Integer protectionBefore,
        Integer protectionAfter
) {
}
