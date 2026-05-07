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
        CommanderMetaProfile profile = profiles.get(key(commander, normalizedBracket));
        if (profile == null) {
            LOG.infov("event=meta.profile.not_found commander={0} bracket={1}", commander, normalizedBracket);
        }
        return profile;
    }

    private CommanderMetaProfile buildProfile(List<MetaDeck> group) {
        MetaDeck first = group.getFirst();
        int sampleSize = group.size();
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (MetaDeck deck : group) {
            Set<String> uniqueCards = deck.cards().stream()
                    .map(MetaDeckCard::name)
                    .filter(name -> name != null && !name.isBlank())
                    .collect(Collectors.toSet());
            for (String card : uniqueCards) {
                counts.merge(card, 1, Integer::sum);
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
                        0.0,
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
