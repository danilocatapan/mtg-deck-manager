package com.mtg.service;

import com.mtg.domain.DeckRecommendations;
import com.mtg.domain.RecommendationItem;
import com.mtg.dto.RecommendationParamsDTO;
import com.mtg.dto.CardResponseDTO;
import com.mtg.domain.DeckAnalysis;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import com.mtg.service.rules.HeuristicRules;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class RecommendationService {

    private static final Logger LOG = Logger.getLogger(RecommendationService.class);

    @Inject
    DeckRepository deckRepository;

    @Inject
    DeckAnalysisService deckAnalysisService;

    @Inject
    CardService cardService;

    @Inject
    com.mtg.service.meta.MetaProvider metaProvider;

    @Inject
    com.mtg.service.synergy.SynergyEngine synergyEngine;

    @Inject
    DeckCompleter deckCompleter;

    public DeckRecommendations recommend(Long deckId, RecommendationParamsDTO params) {
        LOG.infov("recommendation.started deckId={0} params={1}", deckId, params);

        Deck deck = deckRepository.findById(deckId);
        if (deck == null) {
            LOG.errorv("recommendation.failed deck not found {0}", deckId);
            throw new NotFoundException("Deck not found");
        }

        // Enforce deck size invariant: commander excluded, max 99 cards
        if (deck.getCards().stream().mapToInt(DeckCard::getQuantity).sum() > 99) {
            throw new IllegalStateException("Deck inválido > 99 cartas");
        }

        DeckAnalysis analysis = deckAnalysisService.analyzeDeck(deckId);

        String bracket = params != null && params.bracket() != null ? params.bracket() : "casual";

        Map<String, Integer> gaps = HeuristicRules.calculateGaps(analysis.rampCount(), analysis.drawCount(), analysis.removalCount(), analysis.averageCmc(), bracket);
        LOG.debugv("recommendation.gaps {0}", gaps);

        Set<String> existingNames = deck.getCards().stream().map(DeckCard::getName).collect(Collectors.toSet());

        List<RecommendationItem> adds = new ArrayList<>();
        List<RecommendationItem> cuts = new ArrayList<>();
        // pool of candidates collected across gap roles (deduped and ranked later)
        List<RecommendationItem> candidatePool = new ArrayList<>();
        List<com.mtg.service.meta.MetaCard> metaCards = metaProvider.getTopCards(deck.getCommander());
        if (metaCards == null) {
            metaCards = List.of();
        }

        final Map<String, CardResponseDTO> cardCache = preloadCardDetails(deck, metaCards);
        java.util.function.Function<String, CardResponseDTO> lookupCard = name -> cardCache.get(normalizeLookupName(name));

        // Determine commander color identity and tags. Prefer persisted deck.colorIdentity when present.
        Set<String> commanderColors = new HashSet<>();
        Set<String> commanderTags = new HashSet<>();
        CardResponseDTO commanderCard = lookupCard.apply(deck.getCommander());
        try {
            if (deck.getColorIdentity() != null && !deck.getColorIdentity().isBlank()) {
                // normalize persisted colorIdentity string into individual color codes
                String s = deck.getColorIdentity().replaceAll("[^A-Za-z]", "").toUpperCase();
                for (char ch : s.toCharArray()) commanderColors.add(String.valueOf(ch));
                if (commanderCard != null) commanderTags = synergyEngine.tagsForCard(commanderCard);
            } else if (commanderCard != null) {
                if (commanderCard.colorIdentity() != null) commanderColors.addAll(commanderCard.colorIdentity());
                commanderTags = synergyEngine.tagsForCard(commanderCard);
            }
        } catch (Exception e) {
            LOG.debugv("event=recommendation.commander.color.lookup.failed {0}", deck.getCommander());
        }

        // if commander tags are empty, try to build a basic CommanderProfile fallback
        if (commanderTags.isEmpty()) {
            com.mtg.domain.CommanderProfile profile = new com.mtg.domain.CommanderProfile(deck.getCommander(), new ArrayList<>(commanderColors), java.util.List.of());
            commanderTags = synergyEngine.tagsForCommanderProfile(profile);
        }

        // Aggregate tags from existing deck cards for synergy context
        Set<String> deckTags = new HashSet<>();
        for (DeckCard dc : deck.getCards()) {
            try {
                CardResponseDTO info = lookupCard.apply(dc.getName());
                if (info != null) {
                    deckTags.addAll(synergyEngine.tagsForCard(info));
                }
            } catch (Exception e) {
                // ignore individual lookup failures
            }
        }

        // Baseline lands: ensure a minimum number of lands are recommended first
        int targetLands = 36;
        int currentLands = 0;
        for (DeckCard dc : deck.getCards()) {
            try {
                CardResponseDTO info = lookupCard.apply(dc.getName());
                if (info != null && info.typeLine() != null && info.typeLine().contains("Land")) {
                    currentLands += dc.getQuantity();
                }
            } catch (Exception ignored) {}
        }

        int missingLands = targetLands - currentLands;
        if (missingLands > 0) {
            List<String> basics = new ArrayList<>();
            if (commanderColors.isEmpty()) basics.add("Plains");
            else {
                for (String c : commanderColors) {
                    switch (c.toUpperCase()) {
                        case "W" -> basics.add("Plains");
                        case "U" -> basics.add("Island");
                        case "B" -> basics.add("Swamp");
                        case "R" -> basics.add("Mountain");
                        case "G" -> basics.add("Forest");
                        default -> basics.add("Plains");
                    }
                }
            }

            Map<String, Integer> basicLandQuantities = new LinkedHashMap<>();
            for (int i = 0; i < missingLands; i++) {
                String pick = basics.get(i % basics.size());
                basicLandQuantities.merge(pick, 1, Integer::sum);
            }

            for (Map.Entry<String, Integer> entry : basicLandQuantities.entrySet()) {
                adds.add(new RecommendationItem(entry.getKey(), "land", "baseline land", 100.0, 0.0, 0.0, 0.0, 0.0, entry.getValue()));
            }
        }

        // Make final snapshots of tags for use inside stream lambdas
        final Set<String> finalCommanderTags = new HashSet<>(commanderTags);
        final Set<String> finalDeckTags = new HashSet<>(deckTags);

        // For each gap role, attempt to fetch candidates from EDHREC first, fallback to Scryfall
        for (Map.Entry<String, Integer> gap : gaps.entrySet()) {
            String role = gap.getKey();
            int need = gap.getValue();
            List<CardResponseDTO> candidates = new ArrayList<>();

            if (!metaCards.isEmpty()) {
                for (com.mtg.service.meta.MetaCard cu : metaCards) {
                    try {
                        CardResponseDTO c = lookupCard.apply(cu.getName());
                        if (c != null && !existingNames.contains(c.name()) && ColorIdentityMatcher.matches(c, commanderColors)) {
                            candidates.add(c);
                        }
                    } catch (Exception e) {
                        LOG.debugv("event=recommendation.meta.lookup.failed name={0}", cu.getName());
                    }
                    if (candidates.size() >= 50) break; // aggressive cap per role from meta
                }
            }

            if (candidates.isEmpty()) {
                LOG.infov("event=recommendation.meta.miss commander={0} role={1} fallback=scryfall", deck.getCommander(), role);
                String query = switch (role) {
                    case "ramp" -> "oracle:\"add {\"";
                    case "draw" -> "oracle:draw";
                    case "removal" -> "oracle:destroy OR oracle:exile";
                    default -> "";
                };

                List<CardResponseDTO> varResults = searchCandidatesByQuery(query, role);
                candidates = varResults.stream()
                    .filter(c -> c.typeLine() != null)
                    .filter(c -> !c.typeLine().contains("Plane"))
                    .filter(c -> !c.typeLine().contains("Scheme"))
                    .filter(c -> !existingNames.contains(c.name()))
                    .filter(c -> ColorIdentityMatcher.matches(c, commanderColors))
                    .limit(50)
                    .collect(Collectors.toList());
            }
            candidates.forEach(candidate -> cardCache.putIfAbsent(normalizeLookupName(candidate.name()), candidate));

            // Score candidates using available meta popularity and heuristics, add to pool
            final List<com.mtg.service.meta.MetaCard> metaForLookup = metaCards == null ? List.of() : metaCards;
                List<RecommendationItem> scored = candidates.parallelStream()
                    .map(c -> {
                        double popularity = 0.0;
                        if (!metaForLookup.isEmpty()) {
                            for (com.mtg.service.meta.MetaCard cu : metaForLookup) {
                                if (cu.getName().equalsIgnoreCase(c.name())) { popularity = cu.getInclusion(); break; }
                            }
                        }
                        // compute tag-based synergy between candidate and deck+commander
                        double synergy = 0.0;
                        try {
                            Set<String> cardTags = synergyEngine.tagsForCard(c);
                            synergy = synergyEngine.computeSynergy(cardTags, finalDeckTags, finalCommanderTags);
                        } catch (Exception e) {
                            synergy = 0.0;
                        }
                        double s = RecommendationScoring.score(c, role, popularity, synergy);
                        // compute a simple efficiency score based on cmc
                        double efficiencyScore = 0.0;
                        try {
                            Double cmcVal = c.cmc();
                            if (cmcVal != null) {
                                if (cmcVal <= 2.0) efficiencyScore = 0.5;
                                else if (cmcVal == 3.0) efficiencyScore = 0.2;
                            }
                        } catch (Exception ignored) {}
                        return new RecommendationItem(c.name(), role, "gap " + role, s, popularity, synergy, efficiencyScore, 0.0);
                    })
                    .limit(200)
                    .collect(Collectors.toList());

            candidatePool.addAll(scored);
        }

        // After collecting candidatePool across roles, dedupe by name keeping highest score,
        // then sort by score (tie-break by lower CMC) and pick top items to fill deck to 99 cards.
        Map<String, RecommendationItem> bestByName = new HashMap<>();
        for (RecommendationItem r : candidatePool) {
            String n = r.name();
            if (!bestByName.containsKey(n) || r.score() > bestByName.get(n).score()) {
                bestByName.put(n, r);
            }
        }

        Comparator<RecommendationItem> rankingComparator = Comparator
                .comparingDouble(RecommendationItem::score).reversed()
                .thenComparingDouble(item -> {
                    try {
                        CardResponseDTO info = lookupCard.apply(item.name());
                        if (info == null) return Double.MAX_VALUE;
                        Double cmc = info.cmc();
                        return cmc != null ? cmc : Double.MAX_VALUE;
                    } catch (Exception e) {
                        return Double.MAX_VALUE;
                    }
                });

        List<RecommendationItem> rankedCandidates = bestByName.values().stream()
                .filter(r -> !existingNames.contains(r.name()))
                .sorted(rankingComparator)
                .collect(Collectors.toList());

        int missing = 99 - deck.getCards().size() - totalQuantity(adds);
        if (missing > 0) {
            List<RecommendationItem> finalAdds = deckCompleter.complete(deck, rankedCandidates, missing);
            adds.addAll(finalAdds);
        }

        // Simple cut suggestions: high CMC cards
        List<DeckCard> sortedByCmcDesc = deck.getCards().stream()
                .sorted(Comparator.comparingInt(DeckCard::getQuantity).reversed())
                .collect(Collectors.toList());

            for (DeckCard dc : sortedByCmcDesc) {
                CardResponseDTO card = lookupCard.apply(dc.getName());
                double cmc = card != null && card.cmc() != null ? card.cmc() : 0.0;
                if (cmc >= 5.0) {
                    cuts.add(new RecommendationItem(dc.getName(), "high_cmc", "reduce curve", 0.1, 0.0, 0.0, 0.0, 0.0));
                }
                if (cuts.size() >= 5) break;
            }

        DeckRecommendations recommendations = new DeckRecommendations(mergeQuantities(adds), mergeQuantities(cuts), gaps);
        LOG.debugv("recommendation.result deckId={0} adds={1} cuts={2}", deckId, adds.size(), cuts.size());
        return recommendations;
    }

    private List<RecommendationItem> mergeQuantities(List<RecommendationItem> items) {
        Map<String, RecommendationItem> merged = new LinkedHashMap<>();

        for (RecommendationItem item : items) {
            String key = item.name() + "\u0000" + item.role() + "\u0000" + item.reason();
            RecommendationItem existing = merged.get(key);
            if (existing == null) {
                merged.put(key, item);
                continue;
            }

            merged.put(key, new RecommendationItem(
                    existing.name(),
                    existing.role(),
                    existing.reason(),
                    Math.max(existing.score(), item.score()),
                    Math.max(existing.metaScore(), item.metaScore()),
                    Math.max(existing.synergyScore(), item.synergyScore()),
                    Math.max(existing.efficiencyScore(), item.efficiencyScore()),
                    Math.max(existing.estimatedPrice(), item.estimatedPrice()),
                    existing.quantity() + item.quantity()
            ));
        }

        return new ArrayList<>(merged.values());
    }

    private int totalQuantity(List<RecommendationItem> items) {
        return items.stream().mapToInt(RecommendationItem::quantity).sum();
    }

    private Map<String, CardResponseDTO> preloadCardDetails(Deck deck, List<com.mtg.service.meta.MetaCard> metaCards) {
        List<String> names = new ArrayList<>();
        names.add(deck.getCommander());
        deck.getCards().stream()
                .map(DeckCard::getName)
                .forEach(names::add);
        metaCards.stream()
                .map(com.mtg.service.meta.MetaCard::getName)
                .forEach(names::add);

        Map<String, CardResponseDTO> cardsByName = new HashMap<>();
        try {
            cardService.findByNames(names).forEach(cardsByName::put);
        } catch (ExternalServiceException exception) {
            String reason = exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage();
            LOG.warnv("event=recommendation.card.preload.failed reason={0}", reason);
        }

        return cardsByName;
    }

    private List<CardResponseDTO> searchCandidatesByQuery(String query, String role) {
        try {
            return cardService.searchByQuery(query);
        } catch (ExternalServiceException e) {
            String reason = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            LOG.warnv("event=recommendation.scryfall.fallback.failed role={0} query={1} reason={2}", role, query, reason);
            return List.of();
        }
    }

    private String normalizeLookupName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
