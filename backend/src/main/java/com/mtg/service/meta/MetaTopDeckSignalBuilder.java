package com.mtg.service.meta;

import com.mtg.dto.CardResponseDTO;
import com.mtg.model.MetaDeckBracket;
import com.mtg.model.MetaDeckCardSection;
import com.mtg.model.MetaTopDeck;
import com.mtg.model.MetaTopDeckCard;
import com.mtg.repository.MetaTopDeckRepository;
import com.mtg.service.CardService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class MetaTopDeckSignalBuilder {
    private static final Logger LOG = Logger.getLogger(MetaTopDeckSignalBuilder.class);
    private static final int MIN_SAMPLE_SIZE = 3;
    private static final int MIN_APPEARANCES = 2;
    private static final double MIN_INCLUSION = 0.60;
    private static final int MAX_TOP_CARDS = 100;
    public static final String SOURCE = "meta_top_decks";

    @Inject
    MetaTopDeckRepository topDeckRepository;

    @Inject
    CardService cardService;

    @Inject
    CommanderMetaProfileService profileService;

    @PostConstruct
    void loadPersistedSignals() {
        try {
            refreshProfiles();
        } catch (RuntimeException exception) {
            LOG.warnv(exception, "event=meta.top_deck_profiles.startup_refresh_failed");
        }
    }

    public List<String> refreshProfiles() {
        List<MetaTopDeck> decks = topDeckRepository == null ? List.of() : topDeckRepository.listUsableForProfiles();
        List<CommanderMetaProfile> profiles = buildProfiles(decks);
        if (profileService != null) {
            profileService.replaceTopDeckProfiles(profiles);
        }
        List<String> affected = profiles.stream()
                .map(profile -> profile.commander() + "|" + profile.bracket())
                .toList();
        LOG.infov("event=meta.top_deck_profiles.refreshed profiles={0} decks={1}", affected.size(), decks.size());
        return affected;
    }

    public List<CommanderMetaProfile> buildProfiles(List<MetaTopDeck> decks) {
        if (decks == null || decks.isEmpty()) {
            return List.of();
        }
        return decks.stream()
                .filter(deck -> deck.getCommander() != null && !deck.getCommander().isBlank())
                .filter(deck -> deck.getBracket() != null && deck.getBracket() != MetaDeckBracket.UNKNOWN)
                .filter(deck -> deck.getCards() != null && !deck.getCards().isEmpty())
                .collect(Collectors.groupingBy(deck -> normalize(deck.getCommander()) + "|" + recommendationBracket(deck.getBracket())))
                .values()
                .stream()
                .map(this::buildProfile)
                .filter(profile -> profile != null && profile.sampleSize() >= MIN_SAMPLE_SIZE && !profile.topCards().isEmpty())
                .sorted(Comparator.comparing(CommanderMetaProfile::commander).thenComparing(CommanderMetaProfile::bracket))
                .toList();
    }

    private CommanderMetaProfile buildProfile(List<MetaTopDeck> group) {
        if (group.size() < MIN_SAMPLE_SIZE) {
            return null;
        }
        MetaTopDeck first = group.getFirst();
        int sampleSize = group.size();
        Map<String, String> displayNames = new LinkedHashMap<>();
        Map<String, Integer> appearances = new LinkedHashMap<>();
        Map<String, Double> performance = new LinkedHashMap<>();
        Set<String> candidateNames = new LinkedHashSet<>();

        for (MetaTopDeck deck : group) {
            Set<String> uniqueCards = deck.getCards().stream()
                    .filter(card -> card.getSection() == MetaDeckCardSection.MAIN)
                    .filter(card -> card.getName() != null && !card.getName().isBlank())
                    .map(MetaTopDeckCard::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            double deckWeight = deckWeight(deck);
            for (String cardName : uniqueCards) {
                String normalized = normalize(cardName);
                displayNames.putIfAbsent(normalized, cardName.trim());
                candidateNames.add(cardName.trim());
                appearances.merge(normalized, 1, Integer::sum);
                performance.merge(normalized, deckWeight, Double::sum);
            }
        }

        Map<String, CardResponseDTO> resolved = resolve(candidateNames);
        List<MetaCard> topCards = appearances.entrySet().stream()
                .filter(entry -> {
                    double inclusion = entry.getValue() / (double) sampleSize;
                    return entry.getValue() >= MIN_APPEARANCES || inclusion >= MIN_INCLUSION;
                })
                .filter(entry -> resolved.containsKey(cardService.normalizeLookupName(displayNames.get(entry.getKey()))))
                .map(entry -> {
                    String name = displayNames.get(entry.getKey());
                    double inclusion = entry.getValue() / (double) sampleSize;
                    return new MetaCard(
                            name,
                            inclusion,
                            null,
                            resolved.get(cardService.normalizeLookupName(name)).cmc(),
                            entry.getValue(),
                            bracketWeight(first.getBracket()),
                            averagePerformance(entry.getKey(), performance, sampleSize),
                            List.of(),
                            SOURCE
                    );
                })
                .sorted(Comparator
                        .comparingDouble(MetaCard::getInclusion).reversed()
                        .thenComparing(Comparator.comparingInt(MetaCard::getCount).reversed())
                        .thenComparing(MetaCard::getName))
                .limit(MAX_TOP_CARDS)
                .toList();

        if (topCards.isEmpty()) {
            return null;
        }
        String bracket = recommendationBracket(first.getBracket());
        return new CommanderMetaProfile(
                first.getCommander(),
                bracket,
                "competitive_meta",
                sampleSize,
                topCards,
                RoleTargets.forBracket(bracket).asMap(),
                group.stream().map(deck -> deck.getArchetype().name()).distinct().toList(),
                List.of(SOURCE),
                OffsetDateTime.now()
        );
    }

    private Map<String, CardResponseDTO> resolve(Set<String> names) {
        if (names == null || names.isEmpty() || cardService == null) {
            return Map.of();
        }
        try {
            return cardService.findByNames(new ArrayList<>(names));
        } catch (RuntimeException exception) {
            LOG.warnv(exception, "event=meta.top_deck_profiles.resolve_failed names={0}", names.size());
            return Map.of();
        }
    }

    private double deckWeight(MetaTopDeck deck) {
        double rankWeight = deck.getRank() <= 1 ? 0.20 : deck.getRank() <= 3 ? 0.14 : deck.getRank() <= 10 ? 0.08 : 0.03;
        double recencyWeight = 0.0;
        if (deck.getRankingDate() != null) {
            long ageDays = Math.max(0, ChronoUnit.DAYS.between(deck.getRankingDate(), LocalDate.now()));
            recencyWeight = ageDays <= 45 ? 0.10 : ageDays <= 120 ? 0.06 : 0.02;
        }
        double popularityWeight = deck.getPopularityScore() == null ? 0.0 : Math.min(0.08, Math.max(0.0, deck.getPopularityScore()) / 1000.0);
        double winRateWeight = 0.0;
        if (deck.getWins() != null && deck.getLosses() != null && deck.getWins() + deck.getLosses() > 0) {
            double winRate = deck.getWins() / (double) (deck.getWins() + deck.getLosses());
            winRateWeight = Math.min(0.08, Math.max(0.0, winRate - 0.5) * 0.16);
        }
        return Math.min(0.40, rankWeight + recencyWeight + popularityWeight + winRateWeight);
    }

    private double averagePerformance(String card, Map<String, Double> performance, int sampleSize) {
        if (sampleSize <= 0) {
            return 0.0;
        }
        return Math.min(0.40, performance.getOrDefault(card, 0.0) / sampleSize);
    }

    private double bracketWeight(MetaDeckBracket bracket) {
        return switch (bracket) {
            case BRACKET_5 -> 1.25;
            case BRACKET_4, BRACKET_3 -> 1.15;
            case BRACKET_2 -> 1.05;
            default -> 1.0;
        };
    }

    private String recommendationBracket(MetaDeckBracket bracket) {
        if (bracket == null) {
            return "casual";
        }
        return switch (bracket) {
            case BRACKET_1 -> "casual";
            case BRACKET_2 -> "mid";
            case BRACKET_3, BRACKET_4 -> "high-power";
            case BRACKET_5 -> "cedh";
            default -> "casual";
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
