package com.mtg.service;

import com.mtg.domain.RecommendationItem;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class DeckCompleter {

    // per-role caps for additions (can be tuned)
    private static final int MAX_RAMP_ADDS = 10;
    private static final int MAX_DRAW_ADDS = 10;
    private static final int MAX_REMOVAL_ADDS = 10;
    private static final int DEFAULT_ROLE_MAX = 10;

    public List<RecommendationItem> complete(Deck deck, List<RecommendationItem> rankedCandidates, int missing) {
        if (missing <= 0) return List.of();

        Map<String, Integer> addedByRole = new HashMap<>();
        List<RecommendationItem> picks = new ArrayList<>();

        // iterate in order and respect per-role caps and dedupe
        Set<String> seen = new HashSet<>();
        Set<String> existing = deck.getCards().stream().map(DeckCard::getName).collect(Collectors.toSet());

        for (RecommendationItem cand : rankedCandidates) {
            if (picks.size() >= missing) break;
            String name = cand.name();
            if (existing.contains(name) || seen.contains(name)) continue;

            String role = cand.role() != null ? cand.role().toLowerCase() : "";
            int roleMax = switch (role) {
                case "ramp" -> MAX_RAMP_ADDS;
                case "draw" -> MAX_DRAW_ADDS;
                case "removal" -> MAX_REMOVAL_ADDS;
                default -> DEFAULT_ROLE_MAX;
            };

            int currentlyAddedForRole = addedByRole.getOrDefault(role, 0);
            if (currentlyAddedForRole >= roleMax) continue;

            picks.add(cand);
            seen.add(name);
            addedByRole.put(role, currentlyAddedForRole + 1);
        }

        return picks;
    }
}
