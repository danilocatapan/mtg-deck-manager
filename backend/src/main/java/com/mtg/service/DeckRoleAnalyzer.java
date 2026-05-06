package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.service.synergy.SynergyEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class DeckRoleAnalyzer {

    @Inject
    SynergyEngine synergyEngine;

    public DeckRoleSummary analyze(Deck deck, Map<String, CardResponseDTO> cardsByName, String bracket) {
        int totalCards = deck.getCards().stream().mapToInt(DeckCard::getQuantity).sum();
        int lands = 0;
        int ramp = 0;
        int draw = 0;
        int removal = 0;
        int protection = 0;
        int boardWipes = 0;
        int finishers = 0;
        double cmcSum = 0.0;

        for (DeckCard deckCard : deck.getCards()) {
            CardResponseDTO card = cardsByName.get(normalize(deckCard.getName()));
            int qty = deckCard.getQuantity();
            if (card == null) {
                continue;
            }

            String typeLine = text(card.typeLine());
            String oracle = text(card.oracleText());
            double cmc = card.cmc() != null ? card.cmc() : 0.0;
            cmcSum += cmc * qty;

            if (typeLine.contains("land")) lands += qty;
            if (isRamp(card)) ramp += qty;
            if (oracle.contains("draw") || oracle.contains("look at the top") || oracle.contains("impulse")) draw += qty;
            if (oracle.contains("destroy") || oracle.contains("exile") || oracle.contains("counter target") || oracle.contains("return target")) removal += qty;
            if (oracle.contains("indestructible") || oracle.contains("hexproof") || oracle.contains("protection") || oracle.contains("phase out")) protection += qty;
            if (oracle.contains("destroy all") || oracle.contains("exile all") || oracle.contains("all creatures")) boardWipes += qty;
            if (cmc >= 5.0 && (typeLine.contains("creature") || oracle.contains("win the game") || oracle.contains("double"))) finishers += qty;
        }

        double averageCmc = totalCards > 0 ? cmcSum / totalCards : 0.0;
        Map<String, Integer> gaps = detectGaps(lands, ramp, draw, removal, protection, finishers, averageCmc, bracket);
        Set<String> deckTags = synergyEngine.aggregateTags(cardsByName.values().stream().toList());

        return new DeckRoleSummary(totalCards, lands, ramp, draw, removal, protection, boardWipes, finishers, averageCmc, gaps, deckTags);
    }

    private Map<String, Integer> detectGaps(int lands, int ramp, int draw, int removal, int protection, int finishers, double averageCmc, String bracket) {
        String normalizedBracket = bracket == null ? "casual" : bracket.toLowerCase();
        int landTarget = switch (normalizedBracket) {
            case "cedh" -> 27;
            case "high-power" -> 30;
            case "mid" -> 34;
            default -> 36;
        };
        int rampTarget = switch (normalizedBracket) {
            case "cedh" -> 14;
            case "high-power" -> 12;
            case "mid" -> 10;
            default -> 9;
        };
        int drawTarget = switch (normalizedBracket) {
            case "cedh" -> 12;
            case "high-power" -> 10;
            case "mid" -> 9;
            default -> 8;
        };
        int removalTarget = switch (normalizedBracket) {
            case "cedh" -> 14;
            case "high-power" -> 10;
            case "mid" -> 8;
            default -> 7;
        };
        int protectionTarget = "high-power".equals(normalizedBracket) || "cedh".equals(normalizedBracket) ? 4 : 2;

        Map<String, Integer> gaps = new HashMap<>();
        if (lands < landTarget) gaps.put("land", landTarget - lands);
        if (ramp < rampTarget) gaps.put("ramp", rampTarget - ramp);
        if (draw < drawTarget) gaps.put("draw", drawTarget - draw);
        if (removal < removalTarget) gaps.put("removal", removalTarget - removal);
        if (protection < protectionTarget) gaps.put("protection", protectionTarget - protection);
        if (finishers < 4) gaps.put("finisher", 4 - finishers);
        if (averageCmc > 3.8) gaps.put("curve", (int) Math.ceil((averageCmc - 3.6) * 4));
        return gaps;
    }

    private boolean isRamp(CardResponseDTO card) {
        String typeLine = text(card.typeLine());
        String oracle = text(card.oracleText());
        return oracle.contains("add ") || oracle.contains("search your library for a land")
                || typeLine.contains("land") && oracle.contains("add {");
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
