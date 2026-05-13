package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.service.meta.RoleTargets;
import com.mtg.service.synergy.SynergyEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class DeckRoleAnalyzer {

    @Inject
    SynergyEngine synergyEngine;

    public DeckRoleSummary analyze(Deck deck, Map<String, CardResponseDTO> cardsByName, String bracket) {
        List<DeckCard> mainDeckCards = mainDeckCards(deck);
        int totalCards = mainDeckCards.stream().mapToInt(DeckCard::getQuantity).sum();
        int lands = 0;
        int ramp = 0;
        int draw = 0;
        int removal = 0;
        int protection = 0;
        int boardWipes = 0;
        int finishers = 0;
        double cmcSum = 0.0;
        Set<String> deckTags = new HashSet<>();

        for (DeckCard deckCard : mainDeckCards) {
            CardResponseDTO card = cardsByName.get(normalize(deckCard.getName()));
            int qty = deckCard.getQuantity();
            if (card == null) {
                continue;
            }

            String typeLine = text(card.typeLine());
            String oracle = text(card.oracleText());
            double cmc = card.cmc() != null ? card.cmc() : 0.0;
            cmcSum += cmc * qty;
            Set<String> tags = synergyEngine.tagsForCard(card);
            deckTags.addAll(tags);

            if (typeLine.contains("land")) lands += qty;
            if (tags.contains("ramp") || tags.contains("treasure") || tags.contains("mana-rock") || isRamp(card)) ramp += qty;
            if (tags.contains("draw") || tags.contains("impulse-draw") || oracle.contains("look at the top") || oracle.contains("impulse")) draw += qty;
            if (tags.contains("removal") || tags.contains("stack-interaction")) removal += qty;
            if (tags.contains("protection")) protection += qty;
            if (oracle.contains("destroy all") || oracle.contains("exile all") || oracle.contains("all creatures")) boardWipes += qty;
            if (cmc >= 5.0 && (typeLine.contains("creature") || oracle.contains("win the game") || oracle.contains("double"))) finishers += qty;
        }

        double averageCmc = totalCards > 0 ? cmcSum / totalCards : 0.0;
        Map<String, Integer> gaps = detectGaps(lands, ramp, draw, removal, protection, finishers, averageCmc, bracket, deckTags);

        return new DeckRoleSummary(totalCards, lands, ramp, draw, removal, protection, boardWipes, finishers, averageCmc, gaps, deckTags);
    }

    private Map<String, Integer> detectGaps(int lands, int ramp, int draw, int removal, int protection, int finishers, double averageCmc, String bracket, Set<String> deckTags) {
        RoleTargets targets = RoleTargets.forBracket(bracket);
        int landTarget = targets.minLands();
        int rampTarget = targets.ramp();
        int drawTarget = targets.draw();
        int removalTarget = targets.removal();
        int protectionTarget = targets.protection();
        int finisherTarget = 4;

        if (deckTags.contains("big-creature") || deckTags.contains("combat") || deckTags.contains("trample")) {
            rampTarget += 1;
            protectionTarget += 1;
            finisherTarget += 2;
        }
        if (deckTags.contains("graveyard") || deckTags.contains("recursion") || deckTags.contains("self-mill")) {
            drawTarget += 1;
            removalTarget = Math.max(6, removalTarget - 1);
        }
        if (deckTags.contains("sacrifice") || deckTags.contains("sacrifice-outlet")) {
            drawTarget += 1;
            finisherTarget = Math.max(3, finisherTarget - 1);
        }
        if (deckTags.contains("stack-interaction")) {
            removalTarget += 1;
        }

        Map<String, Integer> gaps = new HashMap<>();
        if (lands < landTarget) gaps.put("land", landTarget - lands);
        if (ramp < rampTarget) gaps.put("ramp", rampTarget - ramp);
        if (draw < drawTarget) gaps.put("draw", drawTarget - draw);
        if (removal < removalTarget) gaps.put("removal", removalTarget - removal);
        if (protection < protectionTarget) gaps.put("protection", protectionTarget - protection);
        if (finishers < finisherTarget) gaps.put("finisher", finisherTarget - finishers);
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

    private List<DeckCard> mainDeckCards(Deck deck) {
        return deck.getCards();
    }
}
