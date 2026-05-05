package com.mtg.service;

import com.mtg.domain.DeckAnalysis;
import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DeckAnalysisService {

    private static final Logger LOG = Logger.getLogger(DeckAnalysisService.class);

    private final DeckRepository deckRepository;
    private final CardService cardService;
    private final ClassificationService classificationService;

    @Inject
    public DeckAnalysisService(DeckRepository deckRepository, CardService cardService, ClassificationService classificationService) {
        this.deckRepository = deckRepository;
        this.cardService = cardService;
        this.classificationService = classificationService;
    }

    public DeckAnalysis analyzeDeck(Long id) {
        LOG.infov("analysis.started deckId={0}", id);
        Deck deck = deckRepository.findById(id);
        if (deck == null) {
            LOG.errorv("analysis.failed deck not found {0}", id);
            throw new NotFoundException("Deck not found");
        }

        List<DeckCard> cards = deck.getCards();
        LOG.debugv("analysis.cardCount deckId={0} count={1}", id, cards.size());

        int totalCards = cards.stream().mapToInt(DeckCard::getQuantity).sum();
        double cmcSum = 0.0;
        int rampCount = 0;
        int drawCount = 0;
        int removalCount = 0;
        Map<Integer, Integer> manaCurve = new HashMap<>();

        // per-execution cache to avoid repeated remote lookups
        Map<String, CardResponseDTO> cache = new HashMap<>();

        for (DeckCard dc : cards) {
            String name = dc.getName();
            int qty = dc.getQuantity();

            CardResponseDTO card = cache.computeIfAbsent(name, n -> {
                try {
                    List<CardResponseDTO> results = cardService.searchByName(n);
                    return results.isEmpty() ? null : results.get(0);
                } catch (Exception e) {
                    return null;
                }
            });

            double cmc = card != null && card.cmc() != null ? card.cmc() : 0.0;

            cmcSum += cmc * qty;

            int cmcKey = (int) Math.round(cmc);
            manaCurve.merge(cmcKey, qty, Integer::sum);

            ClassificationService.CardCategory cat = classificationService.classify(card != null ? card.oracleText() : null);
            switch (cat) {
                case RAMP -> rampCount += qty;
                case DRAW -> drawCount += qty;
                case REMOVAL -> removalCount += qty;
                default -> {}
            }
        }

        double averageCmc = totalCards > 0 ? cmcSum / (double) totalCards : 0.0;

        DeckAnalysis analysis = new DeckAnalysis(averageCmc, totalCards, rampCount, drawCount, removalCount, manaCurve);
        LOG.debugv("analysis.result deckId={0} {1}", id, analysis);
        return analysis;
    }
}
