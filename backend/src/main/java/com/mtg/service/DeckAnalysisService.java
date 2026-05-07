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
import java.util.Objects;

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

        return analyze(deck, id);
    }

    public DeckAnalysis analyzeDeck(Long id, String ownerId) {
        LOG.infov("analysis.started deckId={0} owner={1}", id, ownerId);
        Deck deck = deckRepository.findByIdAndOwner(id, ownerId);
        if (deck == null) {
            LOG.errorv("analysis.failed deck not found or not owned {0}", id);
            throw new NotFoundException("Deck not found");
        }

        return analyze(deck, id);
    }

    private DeckAnalysis analyze(Deck deck, Long id) {
        List<DeckCard> cards = deck.getCards();
        LOG.debugv("analysis.cardCount deckId={0} count={1}", id, cards.size());

        int totalCards = cards.stream().mapToInt(DeckCard::getQuantity).sum();
        double cmcSum = 0.0;
        int rampCount = 0;
        int drawCount = 0;
        int removalCount = 0;
        Map<Integer, Integer> manaCurve = new HashMap<>();

        Map<String, CardResponseDTO> cardDetailsByName = cardService.findByNames(cards.stream()
                .map(DeckCard::getName)
                .filter(Objects::nonNull)
                .toList());

        for (DeckCard dc : cards) {
            String name = dc.getName();
            int qty = dc.getQuantity();

            CardResponseDTO card = cardDetailsByName.get(cardService.normalizeLookupName(name));

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
