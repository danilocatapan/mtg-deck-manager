package com.mtg.service.meta;

import java.util.Map;

public record RoleTargets(
        int minLands,
        int maxLands,
        int ramp,
        int draw,
        int removal,
        int protection
) {
    public static RoleTargets forBracket(String bracket) {
        return switch (bracket == null ? "casual" : bracket.toLowerCase()) {
            case "cedh" -> new RoleTargets(27, 31, 14, 12, 14, 4);
            case "high-power" -> new RoleTargets(30, 34, 12, 10, 10, 4);
            case "mid" -> new RoleTargets(34, 37, 10, 9, 8, 3);
            default -> new RoleTargets(36, 38, 9, 8, 7, 2);
        };
    }

    public Map<String, Integer> asMap() {
        return Map.of(
                "minLands", minLands,
                "maxLands", maxLands,
                "ramp", ramp,
                "draw", draw,
                "removal", removal,
                "protection", protection
        );
    }
}
