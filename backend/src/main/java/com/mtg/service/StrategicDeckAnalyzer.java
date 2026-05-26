package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class StrategicDeckAnalyzer {

    @Inject
    ComboDetectionService comboDetectionService;

    @Inject
    CardRoleClassifier roleClassifier;

    public StrategicDeckAssessment assess(
            Deck deck,
            Map<String, CardResponseDTO> cardsByName,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            String bracket
    ) {
        StrategicDeckAssessment.Builder builder = StrategicDeckAssessment.builder();
        String normalizedBracket = normalizeBracket(bracket);
        double curveTarget = curveTarget(normalizedBracket);

        if (roles.averageCmc() > curveTarget + 0.25) {
            builder.issue("high-curve").priority("ramp", 1.18).priority("tutor", 1.08);
        }

        for (Map.Entry<String, Integer> gap : roles.gaps().entrySet()) {
            builder.issue("low-" + gap.getKey()).priority(gap.getKey(), 1.15 + Math.min(0.35, gap.getValue() * 0.06));
        }

        int slowRamp = 0;
        int tappedLands = 0;
        int conditionalDraw = 0;
        int genericThreats = 0;

        for (DeckCard deckCard : deck.getCards()) {
            CardResponseDTO card = cardsByName.get(normalize(deckCard.getName()));
            if (card == null) {
                card = fallbackCard(deckCard);
            }
            String role = classifier().primaryRole(card);
            if (isSlowRamp(card, role, normalizedBracket)) {
                slowRamp += deckCard.getQuantity();
                builder.weakCard(card.name());
            }
            if (isTappedLand(card)) {
                tappedLands += deckCard.getQuantity();
                builder.weakCard(card.name());
            }
            if (isConditionalDraw(card, normalizedBracket)) {
                conditionalDraw += deckCard.getQuantity();
                builder.weakCard(card.name());
            }
            if (isGenericExpensiveThreat(card, profile, normalizedBracket)) {
                genericThreats += deckCard.getQuantity();
                builder.weakCard(card.name());
            }
        }

        if (slowRamp > 0) {
            builder.issue("slow-ramp").priority("ramp", 1.25);
        }
        if (tappedLands > 0 && isOptimizedBracket(normalizedBracket)) {
            builder.issue("tapped-lands").priority("land", 1.18).priority("ramp", 1.08);
        }
        if (conditionalDraw > 0) {
            builder.issue("conditional-draw").priority("draw", 1.22);
        }
        if (genericThreats > 1 || roles.finishers() < finisherTarget(profile)) {
            builder.issue("low-inevitability").priority("finisher", 1.20).priority("combo-piece", 1.22);
        }
        if (comboService().completionSignals(deckNamesWithCommander(deck)).size() > 0) {
            builder.issue("low-combo-redundancy").priority("combo-piece", 1.30);
        }
        if (isOptimizedBracket(normalizedBracket)) {
            builder.priority("tutor", 1.10).priority("selection", 1.08);
        }

        return builder.build();
    }

    private boolean isSlowRamp(CardResponseDTO card, String role, String bracket) {
        if (!"ramp".equals(role)) {
            return false;
        }
        double cmc = card.cmc() == null ? 0.0 : card.cmc();
        String oracle = text(card.oracleText());
        String type = text(card.typeLine());
        if (isOptimizedBracket(bracket) && cmc >= 3.0) {
            return true;
        }
        return cmc >= 3.0
                && (oracle.contains("basic land") || oracle.contains("enters tapped") || type.contains("artifact"));
    }

    private boolean isTappedLand(CardResponseDTO card) {
        String name = normalize(card.name());
        String oracle = text(card.oracleText());
        String type = text(card.typeLine());
        if (!type.contains("land")) {
            return false;
        }
        return oracle.contains("enters tapped")
                || name.contains("temple of")
                || name.contains("rugged highlands")
                || name.contains("guildgate")
                || name.contains("turf")
                || name.contains("chancery")
                || name.contains("aqueduct")
                || name.contains("sanctuary")
                || name.contains("basilica")
                || name.contains("rot farm")
                || name.contains("garrison");
    }

    private boolean isConditionalDraw(CardResponseDTO card, String bracket) {
        String name = normalize(card.name());
        String oracle = text(card.oracleText());
        double cmc = card.cmc() == null ? 0.0 : card.cmc();
        if (!oracle.contains("draw") && !name.contains("insight") && !name.contains("majesty")) {
            return false;
        }
        return isOptimizedBracket(bracket)
                && (oracle.contains("combat damage")
                || oracle.contains("equal to")
                || oracle.contains("deals damage")
                || name.contains("soul's majesty")
                || name.contains("hunter's insight")
                || cmc >= 5.0);
    }

    private boolean isGenericExpensiveThreat(CardResponseDTO card, CommanderArchetypeProfile profile, String bracket) {
        double cmc = card.cmc() == null ? 0.0 : card.cmc();
        String oracle = text(card.oracleText());
        String type = text(card.typeLine());
        if (!type.contains("creature") || cmc < (isOptimizedBracket(bracket) ? 6.0 : 7.0)) {
            return false;
        }
        if (oracle.contains("extra combat") || oracle.contains("untap all") || oracle.contains("whenever this creature attacks")) {
            return false;
        }
        if (oracle.contains("win the game") || oracle.contains("storm") || oracle.contains("infect")) {
            return false;
        }
        return profile == null || !"reanimator".equals(profile.archetype());
    }

    private int finisherTarget(CommanderArchetypeProfile profile) {
        if (profile == null) {
            return 3;
        }
        return switch (profile.archetype()) {
            case "combat", "voltron", "turbo-combo" -> 5;
            case "control", "stax" -> 2;
            default -> 3;
        };
    }

    private double curveTarget(String bracket) {
        return switch (bracket) {
            case "cedh" -> 2.2;
            case "high-power" -> 3.0;
            case "mid" -> 3.4;
            default -> 3.8;
        };
    }

    private boolean isOptimizedBracket(String bracket) {
        return "high-power".equals(bracket) || "cedh".equals(bracket);
    }

    private CardResponseDTO fallbackCard(DeckCard deckCard) {
        String name = deckCard == null ? "" : deckCard.getName();
        if (Set.of("plains", "island", "swamp", "mountain", "forest", "wastes").contains(normalize(name))) {
            return new CardResponseDTO(name, "", "Basic Land", "Add one mana.", 0.0, java.util.List.of(), java.util.List.of());
        }
        return new CardResponseDTO(name, "", "", "", 4.0, java.util.List.of(), java.util.List.of());
    }

    private ComboDetectionService comboService() {
        return comboDetectionService == null ? new ComboDetectionService() : comboDetectionService;
    }

    private Set<String> deckNamesWithCommander(Deck deck) {
        Set<String> names = deck.getCards().stream()
                .map(DeckCard::getName)
                .collect(java.util.stream.Collectors.toCollection(java.util.HashSet::new));
        if (deck.getCommander() != null && !deck.getCommander().isBlank()) {
            names.add(deck.getCommander());
        }
        return names;
    }

    private CardRoleClassifier classifier() {
        return roleClassifier == null ? new CardRoleClassifier() : roleClassifier;
    }

    private String normalizeBracket(String bracket) {
        return bracket == null || bracket.isBlank() ? "casual" : bracket.trim().toLowerCase(Locale.ROOT);
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
