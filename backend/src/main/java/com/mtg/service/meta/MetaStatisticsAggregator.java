package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class MetaStatisticsAggregator {
    public CommanderMetaProfile aggregate(String commander, String bracket, String sourceMode, List<MetaDeck> decks, List<String> sources) {
        if (decks == null || decks.isEmpty()) {
            return CommanderMetaProfile.empty(commander, bracket, sourceMode);
        }

        Map<String, Long> appearances = decks.stream()
                .flatMap(deck -> deck.cards().stream())
                .collect(Collectors.groupingBy(card -> normalize(card.name()), Collectors.counting()));

        int sampleSize = decks.size();
        List<MetaCard> topCards = appearances.entrySet().stream()
                .map(entry -> new MetaCard(entry.getKey(), entry.getValue() / (double) sampleSize, "value", null, 1.0, 0.0, List.of(), firstSource(sources)))
                .sorted(Comparator.comparingDouble(MetaCard::getInclusion).reversed())
                .toList();

        return new CommanderMetaProfile(
                commander,
                bracket,
                sourceMode,
                sampleSize,
                topCards,
                RoleTargets.forBracket(bracket).asMap(),
                List.of(),
                sources,
                OffsetDateTime.now()
        );
    }

    private String firstSource(List<String> sources) {
        return sources == null || sources.isEmpty() ? "LOCAL" : sources.get(0);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
