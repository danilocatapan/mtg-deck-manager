package com.mtg.service.meta;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class CommanderMetaProfileService {
    private static final Logger LOG = Logger.getLogger(CommanderMetaProfileService.class);
    private static final int MAX_TOP_CARDS = 100;

    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
    private final Map<String, CommanderMetaProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, CommanderMetaProfile> topDeckProfiles = new ConcurrentHashMap<>();

    @Inject
    MetaDatasetService datasetService;

    @Inject
    BracketMetaPolicy bracketMetaPolicy;

    @ConfigProperty(name = "meta.profiles.file", defaultValue = "data/commander-meta-profiles.json")
    Path profilesFile;

    @PostConstruct
    public void load() {
        profiles.clear();
        if (!Files.exists(profilesFile)) {
            return;
        }

        try {
            List<CommanderMetaProfile> loaded = mapper.readValue(
                    profilesFile.toFile(),
                    new TypeReference<List<CommanderMetaProfile>>() {}
            );
            for (CommanderMetaProfile profile : loaded) {
                profiles.put(key(profile.commander(), profile.bracket()), profile);
            }
        } catch (Exception exception) {
            LOG.errorv(exception, "event=meta.profile.load_failed file={0}", profilesFile);
        }
    }

    public int rebuild() {
        LOG.infov("event=meta.profile.rebuild.started");
        List<CommanderMetaProfile> built = buildProfiles(datasetService.findAll());
        profiles.clear();
        for (CommanderMetaProfile profile : built) {
            profiles.put(key(profile.commander(), profile.bracket()), profile);
        }
        save();
        LOG.infov("event=meta.profile.rebuild.completed commanders={0}", profiles.size());
        return profiles.size();
    }

    public List<CommanderMetaProfile> buildProfiles(List<MetaDeck> decks) {
        if (decks == null || decks.isEmpty()) {
            return List.of();
        }

        return decks.stream()
                .filter(deck -> deck.commander() != null && !deck.commander().isBlank())
                .filter(deck -> deck.cards() != null && !deck.cards().isEmpty())
                .collect(Collectors.groupingBy(deck -> key(deck.commander(), normalizeBracket(deck.bracket()))))
                .values()
                .stream()
                .map(this::buildProfile)
                .sorted(Comparator.comparing(CommanderMetaProfile::commander).thenComparing(CommanderMetaProfile::bracket))
                .toList();
    }

    public CommanderMetaProfile find(String commander, String bracket) {
        String normalizedBracket = normalizeBracket(bracket);
        CommanderMetaProfile topDeckProfile = topDeckProfiles.get(key(commander, normalizedBracket));
        if (topDeckProfile != null && topDeckProfile.sampleSize() >= 3) {
            return topDeckProfile;
        }
        CommanderMetaProfile profile = profiles.get(key(commander, normalizedBracket));
        if (profile == null) {
            LOG.infov("event=meta.profile.not_found commander={0} bracket={1}", commander, normalizedBracket);
        }
        return profile;
    }

    public CommanderMetaProfile findByCommanderAndBracket(String commander, String bracket) {
        return find(commander, bracket);
    }

    public void replaceTopDeckProfiles(List<CommanderMetaProfile> importedProfiles) {
        topDeckProfiles.clear();
        if (importedProfiles == null || importedProfiles.isEmpty()) {
            LOG.infov("event=meta.top_deck_profiles.replaced profiles=0");
            return;
        }
        for (CommanderMetaProfile profile : importedProfiles) {
            if (profile != null && profile.commander() != null && profile.bracket() != null) {
                topDeckProfiles.put(key(profile.commander(), profile.bracket()), profile);
            }
        }
        LOG.infov("event=meta.top_deck_profiles.replaced profiles={0}", topDeckProfiles.size());
    }

    private CommanderMetaProfile buildProfile(List<MetaDeck> group) {
        MetaDeck first = group.getFirst();
        int sampleSize = group.size();
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Double> performance = new LinkedHashMap<>();

        for (MetaDeck deck : group) {
            Set<String> uniqueCards = deck.cards().stream()
                    .map(MetaDeckCard::name)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toSet());
            double deckPerformance = performanceWeight(deck);
            for (String card : uniqueCards) {
                counts.merge(card, 1, Integer::sum);
                performance.merge(card, deckPerformance, Double::sum);
            }
        }

        List<MetaCard> topCards = counts.entrySet().stream()
                .map(entry -> new MetaCard(
                        entry.getKey(),
                        entry.getValue() / (double) sampleSize,
                        null,
                        null,
                        entry.getValue(),
                        1.0,
                        averagePerformance(entry.getKey(), performance, sampleSize),
                        List.of(),
                        first.source()
                ))
                .sorted(Comparator
                        .comparingDouble(MetaCard::getInclusion).reversed()
                        .thenComparing(Comparator.comparingInt(MetaCard::getCount).reversed())
                        .thenComparing(MetaCard::getName))
                .limit(MAX_TOP_CARDS)
                .toList();

        String bracket = normalizeBracket(first.bracket());
        return new CommanderMetaProfile(
                first.commander(),
                bracket,
                "local_only",
                sampleSize,
                topCards,
                RoleTargets.forBracket(bracket).asMap(),
                List.of(),
                group.stream().map(MetaDeck::source).distinct().toList(),
                OffsetDateTime.now()
        );
    }

    private double averagePerformance(String card, Map<String, Double> performance, int sampleSize) {
        if (sampleSize <= 0) {
            return 0.0;
        }
        double value = Math.min(0.40, performance.getOrDefault(card, 0.0) / sampleSize);
        if (value > 0.0) {
            LOG.infov("event=meta.performance.weighted card=\"{0}\" weight={1} source=TopDeck", card, String.format(Locale.ROOT, "%.2f", value));
        }
        return value;
    }

    private double performanceWeight(MetaDeck deck) {
        if (deck == null || deck.placement() == null) {
            return 0.0;
        }
        double placement = deck.placement() <= 4 ? 0.20 : deck.placement() <= 16 ? 0.12 : 0.04;
        int players = deck.playerCount() == null ? 0 : deck.playerCount();
        double eventSize = players >= 128 ? 0.12 : players >= 64 ? 0.08 : players >= 32 ? 0.04 : 0.0;
        double recency = 0.0;
        if (deck.eventDate() != null && deck.eventDate().isAfter(LocalDate.now().minusDays(90))) {
            recency = 0.08;
        }
        return placement + eventSize + recency;
    }

    private void save() {
        try {
            Path parent = profilesFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(profilesFile.toFile(), profiles.values());
        } catch (Exception exception) {
            LOG.errorv(exception, "event=meta.profile.save_failed file={0}", profilesFile);
        }
    }

    private String key(String commander, String bracket) {
        return normalize(commander) + "|" + normalizeBracket(bracket);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeBracket(String bracket) {
        BracketMetaPolicy policy = bracketMetaPolicy == null ? new BracketMetaPolicy() : bracketMetaPolicy;
        return policy.normalizeBracket(bracket);
    }
}
