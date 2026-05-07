package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class MetaProviderImpl implements MetaProvider {

    @Inject
    MetaDatasetLoader loader;

    @Inject
    BracketMetaPolicy bracketMetaPolicy;

    @Inject
    Instance<MetaSourceAdapter> adapters;

    @Override
    public List<MetaCard> getTopCards(String commander) {
        if (commander == null) return List.of();
        return loader.getCardsForCommander(commander);
    }

    @Override
    public CommanderMetaProfile getCommanderProfile(String commander, String bracket, String sourceMode) {
        if (commander == null || commander.isBlank()) {
            return CommanderMetaProfile.empty(commander, bracket, sourceMode);
        }

        BracketMetaPolicy policy = policy();
        String normalizedBracket = policy.normalizeBracket(bracket);
        String normalizedSourceMode = policy.normalizeSourceMode(sourceMode);
        List<String> sources = policy.sourcesFor(normalizedBracket, normalizedSourceMode);
        List<MetaCard> cards = loader.getCardsForCommander(commander).stream()
                .map(card -> weightForBracket(card, normalizedBracket, sources))
                .sorted((left, right) -> Double.compare(weightedScore(right), weightedScore(left)))
                .toList();

        MetaCommander metaCommander = loader.getDatasetMap().get(normalizeCommanderName(commander));
        List<String> archetypes = metaCommander == null ? List.of() : List.of();
        return new CommanderMetaProfile(
                commander,
                normalizedBracket,
                normalizedSourceMode,
                cards.size(),
                cards,
                RoleTargets.forBracket(normalizedBracket).asMap(),
                archetypes,
                sources,
                OffsetDateTime.now()
        );
    }

    @Override
    public List<MetaSourceStatus> getSourceStatuses() {
        if (adapters != null) {
            List<MetaSourceStatus> adapterStatuses = adapters.stream()
                    .map(MetaSourceAdapter::status)
                    .toList();
            if (!adapterStatuses.isEmpty()) {
                return adapterStatuses;
            }
        }
        return policy().sourceStatuses();
    }

    private MetaCard weightForBracket(MetaCard card, String bracket, List<String> sources) {
        String source = card.getSource();
        if ("LOCAL".equalsIgnoreCase(source) && !sources.isEmpty()) {
            source = sources.get(0);
        }
        double bracketWeight = switch (bracket) {
            case "cedh" -> source.equalsIgnoreCase("EDHTOP16") ? 1.2 : source.equalsIgnoreCase("TOPDECK") ? 1.1 : 1.0;
            case "high-power" -> source.equalsIgnoreCase("TOPDECK") || source.equalsIgnoreCase("SPICERACK") ? 1.15 : 0.8;
            case "mid" -> source.equalsIgnoreCase("EDHREC") ? 1.1 : 0.9;
            default -> source.equalsIgnoreCase("EDHREC") || source.equalsIgnoreCase("LOCAL") ? 1.1 : 0.65;
        };
        double performanceWeight = switch (bracket) {
            case "cedh" -> 0.35;
            case "high-power" -> 0.25;
            case "mid" -> 0.15;
            default -> 0.10;
        };
        return card.withBracketWeight(bracketWeight, performanceWeight, source);
    }

    private double weightedScore(MetaCard card) {
        return card.getInclusion() * card.getBracketWeight() + card.getPerformanceWeight();
    }

    private BracketMetaPolicy policy() {
        return bracketMetaPolicy == null ? new BracketMetaPolicy() : bracketMetaPolicy;
    }

    private String normalizeCommanderName(String commander) {
        String normalized = commander == null ? "" : commander.trim();
        if (normalized.startsWith("A-")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }
}
