package com.mtg.service;

import com.mtg.domain.StrategicRecommendation;
import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import com.mtg.service.meta.MetaCard;
import com.mtg.service.meta.MetaProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class StrategicRecommendationService {

    @Inject
    DeckRepository deckRepository;

    @Inject
    CardService cardService;

    @Inject
    MetaProvider metaProvider;

    @Inject
    DeckRoleAnalyzer deckRoleAnalyzer;

    @Inject
    CommanderArchetypeDetector archetypeDetector;

    @Inject
    CandidateAddSelector addSelector;

    @Inject
    CandidateCutSelector cutSelector;

    @Inject
    RecommendationPairer pairer;

    public List<StrategicRecommendation> recommend(Long deckId, com.mtg.dto.RecommendationParamsDTO params) {
        Deck deck = deckRepository.findById(deckId);
        if (deck == null) {
            throw new NotFoundException("Deck not found");
        }
        if (deck.getCommander() == null || deck.getCommander().isBlank()) {
            throw new IllegalStateException("Commander is required for strategic recommendations");
        }
        int mainDeckCount = deck.getCards().stream().mapToInt(DeckCard::getQuantity).sum();
        if (mainDeckCount > 99) {
            throw new IllegalStateException("Commander deck main deck must have at most 99 cards");
        }

        String bracket = params != null && params.bracket() != null ? params.bracket() : "casual";
        List<MetaCard> metaCards = metaProvider.getTopCards(deck.getCommander());
        if (metaCards == null) metaCards = List.of();

        Map<String, CardResponseDTO> knownCards = preloadCards(deck, metaCards);
        CardResponseDTO commanderCard = knownCards.get(normalize(deck.getCommander()));
        DeckRoleSummary roles = deckRoleAnalyzer.analyze(deck, knownCards, bracket);
        CommanderArchetypeProfile profile = archetypeDetector.detect(deck.getCommander(), commanderCard, roles, persistedColors(deck.getColorIdentity()));

        List<StrategicCandidate> adds = addSelector.select(deck, metaCards, knownCards, profile, roles, bracket);
        List<StrategicCandidate> cuts = cutSelector.select(deck, knownCards, profile, roles);

        return pairer.pair(adds, cuts, profile, roles).stream()
                .limit(5)
                .toList();
    }

    private Map<String, CardResponseDTO> preloadCards(Deck deck, List<MetaCard> metaCards) {
        List<String> names = new ArrayList<>();
        names.add(deck.getCommander());
        deck.getCards().stream().map(DeckCard::getName).forEach(names::add);
        metaCards.stream().map(MetaCard::getName).forEach(names::add);

        Map<String, CardResponseDTO> knownCards = new HashMap<>();
        try {
            knownCards.putAll(cardService.findByNames(names));
        } catch (ExternalServiceException exception) {
            // Strategic recommendations can still use local metadata and any query fallback that remains available.
        }
        return knownCards;
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private java.util.Set<String> persistedColors(String colorIdentity) {
        if (colorIdentity == null || colorIdentity.isBlank()) {
            return java.util.Set.of();
        }
        java.util.Set<String> colors = new java.util.HashSet<>();
        String normalized = colorIdentity.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        for (char symbol : normalized.toCharArray()) {
            colors.add(String.valueOf(symbol));
        }
        return colors;
    }
}
