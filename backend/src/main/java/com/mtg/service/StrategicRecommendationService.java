package com.mtg.service;

import com.mtg.domain.RecommendationCoverage;
import com.mtg.domain.RecommendationSourceSummary;
import com.mtg.domain.StrategicRecommendation;
import com.mtg.domain.StrategicRecommendationRun;
import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import com.mtg.service.meta.BracketMetaPolicy;
import com.mtg.service.meta.CommanderMetaProfile;
import com.mtg.service.meta.CommanderMetaProfileService;
import com.mtg.service.meta.MetaCard;
import com.mtg.service.meta.MetaProvider;
import com.mtg.service.rules.CommanderGameChangerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    StrategicDeckAnalyzer strategicDeckAnalyzer;

    @Inject
    BracketMetaPolicy bracketMetaPolicy;

    @Inject
    CommanderGameChangerService commanderGameChangerService;

    @Inject
    RecommendationAuditService recommendationAuditService;

    @Inject
    RecommendationAuditContext auditContext;

    @Inject
    UserCollectionService userCollectionService;

    public List<StrategicRecommendation> recommend(Long deckId, com.mtg.dto.RecommendationParamsDTO params) {
        return recommendRun(deckId, params, null).recommendations();
    }

    public List<StrategicRecommendation> recommend(Long deckId, com.mtg.dto.RecommendationParamsDTO params, String ownerId) {
        return recommendRun(deckId, params, ownerId).recommendations();
    }

    public StrategicRecommendationRun recommendRun(Long deckId, com.mtg.dto.RecommendationParamsDTO params) {
        return recommendRun(deckId, params, null);
    }

    public StrategicRecommendationRun recommendRun(Long deckId, com.mtg.dto.RecommendationParamsDTO params, String ownerId) {
        Deck deck = ownerId == null ? deckRepository.findById(deckId) : deckRepository.findByIdAndOwner(deckId, ownerId);
        if (deck == null) {
            throw new NotFoundException("Deck not found");
        }
        if (deck.getCommander() == null || deck.getCommander().isBlank()) {
            throw new IllegalStateException("Commander is required for strategic recommendations");
        }
        if (auditContext != null) {
            auditContext.reset();
        }
        int mainDeckCount = mainDeckCards(deck).stream().mapToInt(DeckCard::getQuantity).sum();
        if (mainDeckCount > 99) {
            throw new IllegalStateException("Commander deck must have at most 99 cards");
        }

        BracketMetaPolicy policy = bracketMetaPolicy == null ? new BracketMetaPolicy() : bracketMetaPolicy;
        String bracket = policy.normalizeBracket(params != null ? params.bracket() : null);
        String sourceMode = policy.normalizeSourceMode(params != null ? params.sourceMode() : null);
        String recommendationMode = normalizeRecommendationMode(params == null ? null : params.strategy());
        Double budget = params == null ? null : params.budget();
        Set<String> filters = filters(params);
        int maxRecommendations = maxRecommendations(params);
        boolean ownedOnlyRequested = params != null && Boolean.TRUE.equals(params.ownedOnly());
        Set<String> ownedCardNames = ownedCardNames(ownerId, ownedOnlyRequested);
        boolean collectionAvailable = !ownedOnlyRequested || !ownedCardNames.isEmpty();

        LOG.infov("event=recommendation.strategic.started deckId={0} bracket={1}", deckId, bracket);

        CommanderMetaProfile metaProfile = commanderMetaProfileService == null
                ? null
                : commanderMetaProfileService.findByCommanderAndBracket(deck.getCommander(), bracket);
        if (!hasUsefulMeta(metaProfile) && metaProvider != null) {
            CommanderMetaProfile providerProfile = metaProvider.getCommanderProfile(deck.getCommander(), bracket, sourceMode);
            if (hasUsefulMeta(providerProfile)) {
                metaProfile = providerProfile;
            }
        }
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
        int requestedCardCount = requestedCardCount(deck, metaCards);
        CardResponseDTO commanderCard = knownCards.get(normalize(deck.getCommander()));
        DeckRoleSummary roles = deckRoleAnalyzer.analyze(deck, knownCards, bracket);
        CommanderArchetypeProfile profile = archetypeDetector.detect(deck.getCommander(), commanderCard, roles, persistedColors(deck.getColorIdentity()));
        StrategicDeckAssessment assessment = analyzer().assess(deck, knownCards, profile, roles, bracket);
        LOG.infov(
                "event=recommendation.strategy.context deckId={0} commander=\"{1}\" colors={2} bracket={3} archetype={4} gaps={5} issues={6} weakCards={7}",
                deckId,
                deck.getCommander(),
                profile.colors(),
                bracket,
                profile.archetype(),
                roles.gaps(),
                assessment.issues(),
                assessment.weakCards()
        );

        List<StrategicCandidate> adds = addSelector.select(deck, metaCards, knownCards, profile, roles, bracket, hasUsefulMeta, recommendationMode, budget, filters, assessment, ownedCardNames);
        List<StrategicCandidate> cuts = cutSelector.select(deck, knownCards, profile, roles, bracket, assessment);

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

        List<StrategicRecommendation> recommendations = pairer.pair(
                adds,
                cuts,
                profile,
                roles,
                maxRecommendations,
                bracket,
                metaProfile.sampleSize(),
                metaProfile.sourcesUsed(),
                recommendationMode,
                budget,
                currentGameChangers(deck)
        );
        LOG.infov(
                "event=recommendation.strategic.completed deckId={0} recommendations={1}",
                deckId,
                recommendations.size()
        );
        persistAudit(deck, ownerId, params, bracket, profile, roles, assessment, recommendations);
        return buildRun(
                deck,
                params,
                bracket,
                sourceMode,
                metaProfile,
                hasUsefulMeta,
                knownCards,
                requestedCardCount,
                mainDeckCount,
                collectionAvailable,
                recommendations
        );
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
            return 10;
        }
        return Math.max(10, Math.min(20, requested));
    }

    private StrategicDeckAnalyzer analyzer() {
        if (strategicDeckAnalyzer == null) {
            strategicDeckAnalyzer = new StrategicDeckAnalyzer();
            strategicDeckAnalyzer.comboDetectionService = new ComboDetectionService();
            strategicDeckAnalyzer.roleClassifier = new CardRoleClassifier();
        }
        return strategicDeckAnalyzer;
    }

    private String normalizeRecommendationMode(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return "consistency";
        }
        String normalized = strategy.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "power", "competitive" -> "power";
            case "budget" -> "budget";
            case "theme", "preserve-theme" -> "theme";
            case "cedh", "cEDH" -> "cedh";
            default -> {
                LOG.infov("event=recommendation.strategy.unsupported requested=\"{0}\" effective=consistency", strategy);
                yield "consistency";
            }
        };
    }

    private StrategicRecommendationRun buildRun(
            Deck deck,
            com.mtg.dto.RecommendationParamsDTO params,
            String bracket,
            String sourceMode,
            CommanderMetaProfile metaProfile,
            boolean hasUsefulMeta,
            Map<String, CardResponseDTO> knownCards,
            int requestedCardCount,
            int mainDeckCount,
            boolean collectionAvailable,
            List<StrategicRecommendation> recommendations
    ) {
        double resolutionRate = requestedCardCount <= 0 ? 0.0 : knownCards.size() / (double) requestedCardCount;
        boolean commanderKnown = knownCards.containsKey(normalize(deck.getCommander()));
        boolean fallbackUsed = !hasUsefulMeta || recommendations.stream().anyMatch(recommendation -> "heuristic_fallback".equals(recommendation.source()));
        List<String> sources = metaProfile == null ? List.of() : metaProfile.sourcesUsed();
        int sampleSize = metaProfile == null ? 0 : metaProfile.sampleSize();
        List<String> limitations = limitations(params, commanderKnown, hasUsefulMeta, sampleSize, resolutionRate, mainDeckCount, deck.getCommander(), collectionAvailable);
        String confidence = confidence(hasUsefulMeta, sampleSize, resolutionRate, mainDeckCount, commanderKnown, params, limitations, collectionAvailable);

        RecommendationCoverage coverage = new RecommendationCoverage(
                commanderKnown,
                hasUsefulMeta,
                sampleSize,
                sources,
                requestedCardCount,
                knownCards.size(),
                resolutionRate,
                mainDeckCount,
                bracket,
                fallbackUsed
        );
        RecommendationSourceSummary sourceSummary = new RecommendationSourceSummary(
                sourceMode,
                sampleSize,
                sources,
                attributionFor(sources),
                hasUsefulMeta,
                fallbackUsed
        );
        return new StrategicRecommendationRun(
                confidence,
                coverage,
                dataFreshness(metaProfile),
                sourceSummary,
                limitations,
                benchmarkStatus(deck.getCommander(), bracket, confidence),
                recommendations
        );
    }

    private List<String> limitations(
            com.mtg.dto.RecommendationParamsDTO params,
            boolean commanderKnown,
            boolean hasUsefulMeta,
            int sampleSize,
            double resolutionRate,
            int mainDeckCount,
            String commander,
            boolean collectionAvailable
    ) {
        List<String> limitations = new ArrayList<>();
        if (!commanderKnown) {
            limitations.add("Comandante sem detalhes completos de carta; color identity e sinergia podem depender do fallback persistido.");
        }
        if (!hasUsefulMeta) {
            limitations.add("Dados meta insuficientes para este comandante/bracket; a recomendacao usa heuristicas conservadoras.");
        } else if (sampleSize < 10) {
            limitations.add("Amostra meta pequena; use as sugestoes como direcao, nao como prova estatistica.");
        }
        if (resolutionRate < 0.75) {
            limitations.add("Parte relevante das cartas nao foi resolvida pela base de cartas; comparacoes de curva, preco e papel podem ficar incompletas.");
        }
        if (mainDeckCount < 90) {
            limitations.add("Deck incompleto; qualidade contra GPT nao e comprovada ate a lista se aproximar de 99 cartas no main deck.");
        }
        if (params != null && Boolean.TRUE.equals(params.ownedOnly()) && !collectionAvailable) {
            limitations.add("Filtro 'apenas cartas que possuo' solicitado, mas nao ha inventario de colecao persistido para validar posse das cartas.");
        } else if (params != null && Boolean.TRUE.equals(params.ownedOnly())) {
            limitations.add("Filtro 'apenas cartas que possuo' aplicado contra a colecao persistida do usuario.");
        }
        if (!benchmarkedCommander(commander)) {
            limitations.add("Este comandante ainda nao esta coberto pelo benchmark interno contra GPT.");
        }
        if (limitations.isEmpty()) {
            limitations.add("Recomendacao coberta por dados suficientes para comparacao interna, ainda assim valide preco, disponibilidade e acordo da mesa.");
        }
        return limitations;
    }

    private String confidence(
            boolean hasUsefulMeta,
            int sampleSize,
            double resolutionRate,
            int mainDeckCount,
            boolean commanderKnown,
            com.mtg.dto.RecommendationParamsDTO params,
            List<String> limitations,
            boolean collectionAvailable
    ) {
        if (Boolean.TRUE.equals(params == null ? null : params.ownedOnly()) && !collectionAvailable) {
            return "low_confidence";
        }
        if (hasUsefulMeta && sampleSize >= 10 && resolutionRate >= 0.85 && mainDeckCount >= 90 && commanderKnown && limitations.size() <= 1) {
            return "high_confidence";
        }
        if ((hasUsefulMeta || resolutionRate >= 0.65) && mainDeckCount >= 60 && commanderKnown) {
            return "medium_confidence";
        }
        return "low_confidence";
    }

    private String benchmarkStatus(String commander, String bracket, String confidence) {
        if (!benchmarkedCommander(commander)) {
            return "not_proven_against_gpt";
        }
        if ("low_confidence".equals(confidence)) {
            return "benchmark_reference_exists_but_current_run_is_low_confidence";
        }
        return "covered_by_internal_benchmark_reference";
    }

    private boolean benchmarkedCommander(String commander) {
        String normalized = normalize(commander);
        return Set.of(
                "xenagos, god of revels",
                "k'rrik, son of yawgmoth",
                "grand arbiter augustin iv",
                "kess, dissident mage"
        ).contains(normalized);
    }

    private String dataFreshness(CommanderMetaProfile metaProfile) {
        if (metaProfile == null || metaProfile.updatedAt() == null) {
            return "unknown";
        }
        return metaProfile.updatedAt().toString();
    }

    private String attributionFor(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            return "Sem fonte meta suficiente; heuristicas locais e regras Commander.";
        }
        if (sources.stream().anyMatch(source -> source != null && (source.equalsIgnoreCase("TOPDECK") || source.equalsIgnoreCase("TOPDECK_GG") || source.equalsIgnoreCase("TopDeck")))) {
            return "Dados de torneio fornecidos por TopDeck.gg; usar com atribuicao visivel quando exibido publicamente.";
        }
        if (sources.stream().anyMatch(source -> source != null && source.equalsIgnoreCase("meta_top_decks"))) {
            return "Dados de top decks importados localmente; respeitar atribuicao da fonte original.";
        }
        return "Dados meta locais/importados combinados com heuristicas do sistema.";
    }

    private int requestedCardCount(Deck deck, List<MetaCard> metaCards) {
        Set<String> names = new HashSet<>();
        names.add(normalize(deck.getCommander()));
        deck.getCards().stream()
                .map(DeckCard::getName)
                .map(this::normalize)
                .filter(name -> !name.isBlank())
                .forEach(names::add);
        metaCards.stream()
                .map(MetaCard::getName)
                .map(this::normalize)
                .filter(name -> !name.isBlank())
                .forEach(names::add);
        return names.size();
    }

    private Set<String> filters(com.mtg.dto.RecommendationParamsDTO params) {
        if (params == null) {
            return Set.of();
        }
        java.util.LinkedHashSet<String> filters = new java.util.LinkedHashSet<>();
        if (Boolean.TRUE.equals(params.avoidSalt())) filters.add("avoid-salt");
        if (Boolean.TRUE.equals(params.avoidTutors())) filters.add("avoid-tutors");
        if (Boolean.TRUE.equals(params.improveMana())) filters.add("improve-mana");
        if (Boolean.TRUE.equals(params.lowerCurve())) filters.add("lower-curve");
        if (Boolean.TRUE.equals(params.moreInteraction())) filters.add("more-interaction");
        if (Boolean.TRUE.equals(params.preserveTheme())) filters.add("preserve-theme");
        return filters;
    }

    private Set<String> ownedCardNames(String ownerId, boolean ownedOnlyRequested) {
        if (!ownedOnlyRequested || ownerId == null || ownerId.isBlank() || userCollectionService == null) {
            return Set.of();
        }
        try {
            return userCollectionService.ownedCardNames(ownerId);
        } catch (Exception exception) {
            LOG.warnv(exception, "event=recommendation.collection.unavailable owner={0}", ownerId);
            return Set.of();
        }
    }

    private int currentGameChangers(Deck deck) {
        if (deck == null || commanderGameChangerService == null) {
            return 0;
        }
        int count = commanderGameChangerService.isGameChanger(deck.getCommander()) ? 1 : 0;
        for (DeckCard card : mainDeckCards(deck)) {
            if (commanderGameChangerService.isGameChanger(card.getName())) {
                count += Math.max(1, card.getQuantity());
            }
        }
        return count;
    }

    private List<DeckCard> mainDeckCards(Deck deck) {
        return deck.getCards();
    }

    private void persistAudit(
            Deck deck,
            String ownerId,
            com.mtg.dto.RecommendationParamsDTO params,
            String bracket,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            StrategicDeckAssessment assessment,
            List<StrategicRecommendation> recommendations
    ) {
        if (recommendationAuditService == null) {
            return;
        }
        try {
            recommendationAuditService.persistRun(
                    deck,
                    ownerId,
                    params,
                    bracket,
                    profile,
                    roles,
                    assessment,
                    recommendations,
                    auditContext
            );
        } catch (Exception exception) {
            LOG.warnv(exception, "event=recommendation.audit.failed deckId={0}", deck == null ? null : deck.getId());
        }
    }
}
