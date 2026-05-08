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
        Integer protectionAfter,
        Integer gameChangersBefore,
        Integer gameChangersAfter,
        Integer bracketPressureBefore,
        Integer bracketPressureAfter
) {
    public RecommendationImpact(
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
        this(
                role,
                averageCmcBefore,
                averageCmcAfter,
                rampBefore,
                rampAfter,
                drawBefore,
                drawAfter,
                removalBefore,
                removalAfter,
                protectionBefore,
                protectionAfter,
                0,
                0,
                0,
                0
        );
    }
}
