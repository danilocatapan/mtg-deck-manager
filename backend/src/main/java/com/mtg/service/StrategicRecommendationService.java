package com.mtg.service;

import com.mtg.domain.StrategicRecommendation;
import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import com.mtg.service.meta.BracketMetaPolicy;
import com.mtg.service.meta.CommanderMetaProfile;
import com.mtg.service.meta.CommanderMetaProfileService;
import com.mtg.service.meta.MetaCard;
import com.mtg.service.meta.MetaProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class StrategicRecommendationService {
    private static final Logger LOG = Logger.getLogger(StrategicRecommendationService.class);

    @Inject
    DeckRepository deckRepository;

    @Inject
    CardService cardService;

    @Inject
    MetaProvider metaProvider;

    @Inject
    CommanderMetaProfileService commanderMetaProfileService;

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

    @Inject
    BracketMetaPolicy bracketMetaPolicy;

    public List<StrategicRecommendation> recommend(Long deckId, com.mtg.dto.RecommendationParamsDTO params) {
        return recommend(deckId, params, null);
    }

    public List<StrategicRecommendation> recommend(Long deckId, com.mtg.dto.RecommendationParamsDTO params, String ownerId) {
        Deck deck = ownerId == null ? deckRepository.findById(deckId) : deckRepository.findByIdAndOwner(deckId, ownerId);
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

        BracketMetaPolicy policy = bracketMetaPolicy == null ? new BracketMetaPolicy() : bracketMetaPolicy;
        String bracket = policy.normalizeBracket(params != null ? params.bracket() : null);
        String sourceMode = policy.normalizeSourceMode(params != null ? params.sourceMode() : null);
        int maxRecommendations = maxRecommendations(params);

        LOG.infov("event=recommendation.strategic.started deckId={0} bracket={1}", deckId, bracket);

        CommanderMetaProfile metaProfile = commanderMetaProfileService == null
                ? null
                : commanderMetaProfileService.findByCommanderAndBracket(deck.getCommander(), bracket);
        boolean hasUsefulMeta = hasUsefulMeta(metaProfile);
        if (hasUsefulMeta) {
            LOG.infov(
                    "event=recommendation.meta_profile.loaded commander={0} bracket={1} sampleSize={2}",
                    deck.getCommander(),
                    bracket,
                    metaProfile.sampleSize()
            );
        } else {
            LOG.infov(
                    "event=recommendation.meta_profile.unavailable commander={0} bracket={1}",
                    deck.getCommander(),
                    bracket
            );
            LOG.infov("event=recommendation.fallback.used reason={0}", metaProfile == null ? "profile_not_found" : "sample_too_small");
            List<MetaCard> legacyCards = metaProvider.getTopCards(deck.getCommander());
            metaProfile = new CommanderMetaProfile(
                    deck.getCommander(),
                    bracket,
                    sourceMode,
                    legacyCards == null ? 0 : legacyCards.size(),
                    legacyCards == null ? List.of() : legacyCards,
                    com.mtg.service.meta.RoleTargets.forBracket(bracket).asMap(),
                    List.of(),
                    policy.sourcesFor(bracket, sourceMode),
                    java.time.OffsetDateTime.now()
            );
        }
        List<MetaCard> metaCards = metaProfile.topCards();
        if (metaCards == null) metaCards = List.of();

        Map<String, CardResponseDTO> knownCards = preloadCards(deck, metaCards);
        CardResponseDTO commanderCard = knownCards.get(normalize(deck.getCommander()));
        DeckRoleSummary roles = deckRoleAnalyzer.analyze(deck, knownCards, bracket);
        CommanderArchetypeProfile profile = archetypeDetector.detect(deck.getCommander(), commanderCard, roles, persistedColors(deck.getColorIdentity()));

        List<StrategicCandidate> adds = addSelector.select(deck, metaCards, knownCards, profile, roles, bracket, hasUsefulMeta);
        List<StrategicCandidate> cuts = cutSelector.select(deck, knownCards, profile, roles, bracket);

        LOG.infov(
                "event=strategic.recommendation.context deckId={0} commander=\"{1}\" bracket={2} sourceMode={3} sampleSize={4} sources={5} fallback={6}",
                deckId,
                deck.getCommander(),
                bracket,
                sourceMode,
                metaProfile.sampleSize(),
                metaProfile.sourcesUsed(),
                !hasUsefulMeta
        );
        LOG.infov(
                "event=recommendation.add_candidates.generated count={0} source={1}",
                adds.size(),
                hasUsefulMeta ? "meta_profile" : "heuristic"
        );
        LOG.infov("event=recommendation.cut_candidates.generated count={0}", cuts.size());

        List<StrategicRecommendation> recommendations = pairer.pair(adds, cuts, profile, roles, maxRecommendations, bracket);
        LOG.infov(
                "event=recommendation.strategic.completed deckId={0} recommendations={1}",
                deckId,
                recommendations.size()
        );
        return recommendations;
    }

    private boolean hasUsefulMeta(CommanderMetaProfile profile) {
        return profile != null
                && profile.sampleSize() >= 3
                && profile.topCards() != null
                && !profile.topCards().isEmpty();
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

    private int maxRecommendations(com.mtg.dto.RecommendationParamsDTO params) {
        Integer requested = params == null ? null : params.maxRecommendations();
        if (requested == null) {
            return 5;
        }
        return Math.max(3, Math.min(5, requested));
    }
}
