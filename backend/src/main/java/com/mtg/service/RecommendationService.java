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
            // try EDHREC meta first
            List<EdhrecService.CardUsage> metaCards = edhrecService.getTopCards(deck.getCommander());
            List<CardResponseDTO> candidates = new ArrayList<>();

            if (!metaCards.isEmpty()) {
                for (EdhrecService.CardUsage cu : metaCards) {
                    try {
                        List<CardResponseDTO> found = cardService.searchByName(cu.name);
                        if (!found.isEmpty()) {
                            CardResponseDTO c = found.get(0);
                            if (!existingNames.contains(c.name()) && ColorIdentityMatcher.matches(c, commanderColors)) {
                                candidates.add(c);
                            }
                        }
                    } catch (Exception e) {
                        LOG.debugv("event=recommendation.meta.lookup.failed name={0}", cu.name);
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

            // Score and pick top 'need' items, using EDHREC popularity when available
            List<RecommendationItem> scored = candidates.stream()
                    .map(c -> {
                        double popularity = 0.0;
                        // attempt to find popularity
                        if (!metaCards.isEmpty()) {
                            for (EdhrecService.CardUsage cu : metaCards) {
                                if (cu.name.equalsIgnoreCase(c.name())) { popularity = cu.inclusionRate; break; }
                            }
                        }
                        double synergy = 0.0; // placeholder for future synergy calc
                        double s = RecommendationScoring.score(c, role, popularity, synergy);
                        return new RecommendationItem(c.name(), role, "gap " + role, s, 0.0);
                    })
                    .sorted(Comparator.comparingDouble(RecommendationItem::score).reversed())
                    .limit(need)
                    .collect(Collectors.toList());

            // ensure diversity: avoid many identical categories
            Map<String, Long> roleCount = scored.stream().collect(Collectors.groupingBy(RecommendationItem::role, Collectors.counting()));
            adds.addAll(scored);
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
