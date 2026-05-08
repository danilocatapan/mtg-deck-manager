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
        return estimate(new BracketEstimateInput(
                mainDeckSize,
                legal,
                averageCmc,
                interactionCount,
                rampCount,
                0,
                0,
                0,
                0,
                0,
                0,
                9,
                null
        ));
    }

    public BracketEstimateDTO estimate(BracketEstimateInput input) {
        if (input == null) {
            return dto(1, "Exhibition", "precon");
        }
        int mainDeckSize = input.mainDeckSize();
        boolean legal = input.legal();
        double averageCmc = input.averageCmc();
        int interactionCount = input.interactionCount();
        int rampCount = input.rampCount();

        if (!legal) {
            return dto(1, "Exhibition", "precon");
        }
        if (mainDeckSize < 90) {
            return dto(1, "Exhibition / Incomplete", "precon");
        }

        int level = 2;
        String label = "Core / Casual";
        String alias = "casual";

        if (interactionCount >= 7 && rampCount >= 8) {
            level = 3;
            label = "Upgraded";
            alias = "mid";
        }
        if (averageCmc <= 3.0 && interactionCount >= 10 && rampCount >= 10) {
            level = 4;
            label = "Optimized";
            alias = "high-power";
        }
        if (averageCmc <= 2.6 && interactionCount >= 12 && rampCount >= 12) {
            level = 5;
            label = "cEDH / Max Power";
            alias = "cedh";
        }

        if (input.gameChangerCount() >= 1 && level < 3) {
            level = 3;
            label = "Upgraded / Game Changers";
            alias = "mid";
        }
        if (input.gameChangerCount() >= 4 && level < 4) {
            level = 4;
            label = "Optimized / Game Changers";
            alias = "high-power";
        }
        if (input.fastManaCount() >= 2 && input.tutorCount() >= 2 && input.comboDensity() >= 2 && level < 4) {
            level = 4;
            label = "Optimized / Fast Combo";
            alias = "high-power";
        }
        if ((input.extraTurnCount() > 0 || input.massLandDestructionCount() > 0) && level < 4) {
            level = 4;
            label = "Optimized / High Impact Effects";
            alias = "high-power";
        }
        if ((input.winTurnEstimate() > 0 && input.winTurnEstimate() <= 4 && interactionCount >= 10)
                || (input.fastManaCount() >= 3 && input.tutorCount() >= 3 && input.comboDensity() >= 2)) {
            return dto(5, "cEDH / Max Power", "cedh");
        }

        int declaredLevel = levelFor(input.declaredIntent());
        if (declaredLevel > level && declaredLevel <= 5) {
            level = declaredLevel;
            alias = normalizeAlias(input.declaredIntent());
            label = switch (alias) {
                case "mid" -> "Upgraded / Intent Declared";
                case "high-power" -> "Optimized / Intent Declared";
                case "cedh" -> "cEDH / Intent Declared";
                default -> label;
            };
        }

        return dto(level, label, alias);
    }

    private BracketEstimateDTO dto(int level, String label, String alias) {
        return new BracketEstimateDTO(level, label, alias);
    }

    public record BracketEstimateInput(
            int mainDeckSize,
            boolean legal,
            double averageCmc,
            int interactionCount,
            int rampCount,
            int gameChangerCount,
            int comboDensity,
            int tutorCount,
            int fastManaCount,
            int extraTurnCount,
            int massLandDestructionCount,
            int winTurnEstimate,
            String declaredIntent
    ) {
    }
}
