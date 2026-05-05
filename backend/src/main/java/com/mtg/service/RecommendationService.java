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
    EdhrecService edhrecService;

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

        DeckAnalysis analysis = deckAnalysisService.analyzeDeck(deckId);

        String bracket = params != null && params.bracket() != null ? params.bracket() : "casual";

        Map<String, Integer> gaps = HeuristicRules.calculateGaps(analysis.rampCount(), analysis.drawCount(), analysis.removalCount(), analysis.averageCmc(), bracket);
        LOG.debugv("recommendation.gaps {0}", gaps);

        Set<String> existingNames = deck.getCards().stream().map(DeckCard::getName).collect(Collectors.toSet());

        List<RecommendationItem> adds = new ArrayList<>();
        List<RecommendationItem> cuts = new ArrayList<>();
        // pool of candidates collected across gap roles (deduped and ranked later)
        List<RecommendationItem> candidatePool = new ArrayList<>();

        // Determine commander color identity and tags
        Set<String> commanderColors = new HashSet<>();
        Set<String> commanderTags = new HashSet<>();
        CardResponseDTO commanderCard = null;
        try {
            List<CardResponseDTO> cmdInfo = cardService.searchByName(deck.getCommander());
            if (!cmdInfo.isEmpty()) {
                commanderCard = cmdInfo.get(0);
                if (commanderCard.colorIdentity() != null) commanderColors.addAll(commanderCard.colorIdentity());
                commanderTags = synergyEngine.tagsForCard(commanderCard);
            }
        } catch (Exception e) {
            LOG.debugv("event=recommendation.commander.color.lookup.failed {0}", deck.getCommander());
        }

        // Aggregate tags from existing deck cards for synergy context
        Set<String> deckTags = new HashSet<>();
        for (DeckCard dc : deck.getCards()) {
            try {
                List<CardResponseDTO> infos = cardService.searchByName(dc.getName());
                if (!infos.isEmpty()) {
                    deckTags.addAll(synergyEngine.tagsForCard(infos.get(0)));
                }
            } catch (Exception e) {
                // ignore individual lookup failures
            }
        }

        // Make final snapshots of tags for use inside stream lambdas
        final Set<String> finalCommanderTags = new HashSet<>(commanderTags);
        final Set<String> finalDeckTags = new HashSet<>(deckTags);

        // For each gap role, attempt to fetch candidates from EDHREC first, fallback to Scryfall
        for (Map.Entry<String, Integer> gap : gaps.entrySet()) {
            String role = gap.getKey();
            int need = gap.getValue();
            // try local MetaProvider first (offline dataset), then EDHREC service as fallback
            List<com.mtg.service.meta.MetaCard> metaCards = metaProvider.getTopCards(deck.getCommander());
            if (metaCards == null || metaCards.isEmpty()) {
                // fallback to EdhrecService (legacy in-memory/fetch)
                List<EdhrecService.CardUsage> edh = edhrecService.getTopCards(deck.getCommander());
                // convert to MetaCard-like usage
                metaCards = edh.stream().map(cu -> new com.mtg.service.meta.MetaCard(cu.name, cu.inclusionRate, cu.category, null)).toList();
            }
            List<CardResponseDTO> candidates = new ArrayList<>();

            if (!metaCards.isEmpty()) {
                for (com.mtg.service.meta.MetaCard cu : metaCards) {
                    try {
                        List<CardResponseDTO> found = cardService.searchByName(cu.getName());
                        if (!found.isEmpty()) {
                            CardResponseDTO c = found.get(0);
                            if (!existingNames.contains(c.name()) && ColorIdentityMatcher.matches(c, commanderColors)) {
                                candidates.add(c);
                            }
                        }
                    } catch (Exception e) {
                        LOG.debugv("event=recommendation.meta.lookup.failed name={0}", cu.getName());
                    }
                    if (candidates.size() >= 100) break;
                }
            }

            if (candidates.isEmpty()) {
                String query = switch (role) {
                    case "ramp" -> "oracle:\"add {\"";
                    case "draw" -> "oracle:draw";
                    case "removal" -> "oracle:destroy OR oracle:exile";
                    default -> "";
                };

                List<CardResponseDTO> varResults = cardService.searchByQuery(query);
                candidates = varResults.stream()
                        .filter(c -> !existingNames.contains(c.name()))
                        .filter(c -> ColorIdentityMatcher.matches(c, commanderColors))
                        .limit(200)
                        .collect(Collectors.toList());
            }

            // Score candidates using available meta popularity and heuristics, add to pool
            final List<com.mtg.service.meta.MetaCard> metaForLookup = metaCards == null ? List.of() : metaCards;
            List<RecommendationItem> scored = candidates.stream()
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
                    .limit(500)
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
                        List<CardResponseDTO> infos = cardService.searchByName(item.name());
                        if (infos.isEmpty()) return Double.MAX_VALUE;
                        Double cmc = infos.get(0).cmc();
                        return cmc != null ? cmc : Double.MAX_VALUE;
                    } catch (Exception e) {
                        return Double.MAX_VALUE;
                    }
                });

        List<RecommendationItem> rankedCandidates = bestByName.values().stream()
                .filter(r -> !existingNames.contains(r.name()))
                .sorted(rankingComparator)
                .collect(Collectors.toList());

        int missing = 99 - deck.getCards().size();
        if (missing > 0) {
            List<RecommendationItem> finalAdds = deckCompleter.complete(deck, rankedCandidates, missing);
            adds.addAll(finalAdds);
        }

        // Simple cut suggestions: high CMC cards
        List<DeckCard> sortedByCmcDesc = deck.getCards().stream()
                .sorted(Comparator.comparingInt(DeckCard::getQuantity).reversed())
                .collect(Collectors.toList());

            for (DeckCard dc : sortedByCmcDesc) {
            List<CardResponseDTO> info = cardService.searchByName(dc.getName());
            CardResponseDTO card = info.isEmpty() ? null : info.get(0);
            double cmc = card != null && card.cmc() != null ? card.cmc() : 0.0;
            if (cmc >= 5.0) {
                cuts.add(new RecommendationItem(dc.getName(), "high_cmc", "reduce curve", 0.1, 0.0, 0.0, 0.0, 0.0));
            }
            if (cuts.size() >= 5) break;
        }

        DeckRecommendations recommendations = new DeckRecommendations(adds, cuts, gaps);
        LOG.debugv("recommendation.result deckId={0} adds={1} cuts={2}", deckId, adds.size(), cuts.size());
        return recommendations;
    }
}
