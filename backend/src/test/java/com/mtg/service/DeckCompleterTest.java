package com.mtg.service;

import com.mtg.domain.RecommendationItem;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DeckCompleterTest {

    @Test
    public void respectsRoleCapsAndFillsMissing() {
        DeckCompleter completer = new DeckCompleter();

        Deck deck = new Deck();
        // deck with zero cards

        // prepare 12 ramp candidates then 5 draw candidates
        List<RecommendationItem> candidates = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            candidates.add(new RecommendationItem("Ramp " + i, "ramp", "gap ramp", 1.0, 0.5, 0.2, 0.1, 0.0));
        }
        for (int i = 1; i <= 5; i++) {
            candidates.add(new RecommendationItem("Draw " + i, "draw", "gap draw", 0.9, 0.4, 0.3, 0.1, 0.0));
        }

        int missing = 12;
        List<RecommendationItem> picks = completer.complete(deck, candidates, missing);

        // should fill 12 items: max 10 ramps, then 2 draws
        assertEquals(12, picks.size());
        long rampCount = picks.stream().filter(p -> "ramp".equalsIgnoreCase(p.role())).count();
        long drawCount = picks.stream().filter(p -> "draw".equalsIgnoreCase(p.role())).count();
        assertEquals(10, rampCount);
        assertEquals(2, drawCount);
    }

    @Test
    public void avoidsExistingAndDuplicates() {
        DeckCompleter completer = new DeckCompleter();

        Deck deck = new Deck();
        List<DeckCard> existing = new ArrayList<>();
        existing.add(new DeckCard("Sol Ring", 1));
        deck.setCards(existing);

        List<RecommendationItem> candidates = new ArrayList<>();
        // include duplicate names and an existing card
        candidates.add(new RecommendationItem("Sol Ring", "ramp", "gap ramp", 1.0,0.5,0.2,0.1,0.0));
        candidates.add(new RecommendationItem("Ramp A", "ramp", "gap ramp", 1.0,0.5,0.2,0.1,0.0));
        candidates.add(new RecommendationItem("Ramp A", "ramp", "gap ramp", 0.9,0.4,0.2,0.1,0.0));
        candidates.add(new RecommendationItem("Draw A", "draw", "gap draw", 0.8,0.3,0.2,0.1,0.0));

        int missing = 3;
        List<RecommendationItem> picks = completer.complete(deck, candidates, missing);

        // should not include Sol Ring and should dedupe Ramp A
        assertFalse(picks.stream().anyMatch(p -> p.name().equalsIgnoreCase("Sol Ring")));
        long rampCount = picks.stream().filter(p -> "ramp".equalsIgnoreCase(p.role())).count();
        long drawCount = picks.stream().filter(p -> "draw".equalsIgnoreCase(p.role())).count();
        assertEquals(2, picks.size());
        assertEquals(1, rampCount);
        assertEquals(1, drawCount);
    }
}
