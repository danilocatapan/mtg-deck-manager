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

        // Determine commander color identity
        Set<String> commanderColors = new HashSet<>();
        try {
            List<CardResponseDTO> cmdInfo = cardService.searchByName(deck.getCommander());
            if (!cmdInfo.isEmpty() && cmdInfo.get(0).colorIdentity() != null) {
                commanderColors.addAll(cmdInfo.get(0).colorIdentity());
            }
        } catch (Exception e) {
            LOG.debugv("event=recommendation.commander.color.lookup.failed {0}", deck.getCommander());
        }

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
            List<RecommendationItem> scored = candidates.stream()
                    .map(c -> {
                        double popularity = 0.0;
                        if (!metaCards.isEmpty()) {
                            for (com.mtg.service.meta.MetaCard cu : metaCards) {
                                if (cu.getName().equalsIgnoreCase(c.name())) { popularity = cu.getInclusion(); break; }
                            }
                        }
                        double synergy = 0.0; // placeholder for future synergy calc
                        double s = RecommendationScoring.score(c, role, popularity, synergy);
                        return new RecommendationItem(c.name(), role, "gap " + role, s, 0.0);
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
            List<RecommendationItem> finalAdds = rankedCandidates.stream().limit(missing).collect(Collectors.toList());
            // ensure no duplicates in adds
            Set<String> seen = new HashSet<>();
            for (RecommendationItem ai : finalAdds) {
                if (!seen.contains(ai.name())) {
                    adds.add(ai);
                    seen.add(ai.name());
                }
            }
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
                cuts.add(new RecommendationItem(dc.getName(), "high_cmc", "reduce curve", 0.1, 0.0));
            }
            if (cuts.size() >= 5) break;
        }

        DeckRecommendations recommendations = new DeckRecommendations(adds, cuts, gaps);
        LOG.debugv("recommendation.result deckId={0} adds={1} cuts={2}", deckId, adds.size(), cuts.size());
        return recommendations;
    }
}
