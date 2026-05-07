package com.mtg.service.rules;

import com.mtg.dto.BracketEstimateDTO;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;

@ApplicationScoped
public class CommanderBracketService {

    public String normalizeAlias(String bracket) {
        if (bracket == null || bracket.isBlank()) {
            return "casual";
        }
        String normalized = bracket.trim().toLowerCase(Locale.ROOT).replace("_", "-");
        if (normalized.equals("highpower")) {
            return "high-power";
        }
        return switch (normalized) {
            case "1", "precon", "exhibition" -> "precon";
            case "2", "casual", "core" -> "casual";
            case "3", "mid", "upgraded" -> "mid";
            case "4", "high-power", "optimized" -> "high-power";
            case "5", "cedh", "cedh/max" -> "cedh";
            default -> "casual";
        };
    }

    public int levelFor(String bracket) {
        return switch (normalizeAlias(bracket)) {
            case "precon" -> 1;
            case "mid" -> 3;
            case "high-power" -> 4;
            case "cedh" -> 5;
            default -> 2;
        };
    }

    public BracketEstimateDTO estimate(int mainDeckSize, boolean legal, double averageCmc, int interactionCount, int rampCount) {
        if (!legal) {
            return dto(1, "Exhibition", "precon");
        }
        if (mainDeckSize < 90) {
            return dto(1, "Exhibition / Incomplete", "precon");
        }
        if (averageCmc <= 2.6 && interactionCount >= 12 && rampCount >= 12) {
            return dto(5, "cEDH / Max Power", "cedh");
        }
        if (averageCmc <= 3.0 && interactionCount >= 10 && rampCount >= 10) {
            return dto(4, "Optimized", "high-power");
        }
        if (interactionCount >= 7 && rampCount >= 8) {
            return dto(3, "Upgraded", "mid");
        }
        return dto(2, "Core / Casual", "casual");
    }

    private BracketEstimateDTO dto(int level, String label, String alias) {
        return new BracketEstimateDTO(level, label, alias);
    }
}
