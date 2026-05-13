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

        // determine available candidates per role (excluding existing) to avoid blocking fills
        Set<String> existing = deck.getCards().stream()
                .map(DeckCard::getName)
                .collect(Collectors.toSet());
        Map<String, Integer> availableByRole = new HashMap<>();
        for (RecommendationItem rc : rankedCandidates) {
            if (existing.contains(rc.name())) continue;
            String r = rc.role() != null ? rc.role().toLowerCase() : "";
            availableByRole.put(r, availableByRole.getOrDefault(r, 0) + 1);
        }

        // iterate in order and respect per-role caps only if there is excess
        Set<String> seen = new HashSet<>();
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

            int available = availableByRole.getOrDefault(role, 0);
            // if available is less than roleMax, relax limit so we can fill
            int effectiveMax = available > 0 && available < roleMax ? available : roleMax;

            int currentlyAddedForRole = addedByRole.getOrDefault(role, 0);
            if (currentlyAddedForRole >= effectiveMax) continue;

            picks.add(cand);
            seen.add(name);
            addedByRole.put(role, currentlyAddedForRole + 1);
        }

        return picks;
    }
}
