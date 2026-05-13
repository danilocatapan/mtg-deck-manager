package com.mtg.service;

import com.mtg.domain.ComboAnalysis;
import com.mtg.domain.DeckAnalysis;
import com.mtg.domain.ExplainableScore;
import com.mtg.domain.ManaBaseAnalysis;
import com.mtg.domain.ProbabilityAnalysis;
import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import com.mtg.service.synergy.CardTagger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class DeckAnalysisService {

    private static final Logger LOG = Logger.getLogger(DeckAnalysisService.class);
    private static final List<String> COLORS = List.of("W", "U", "B", "R", "G", "C");
    private static final Pattern MANA_SYMBOL = Pattern.compile("\\{([WUBRGC])}");

    private final DeckRepository deckRepository;
    private final CardService cardService;
    private final ClassificationService classificationService;
    private final ComboDetectionService comboDetectionService;
    private final CardTagger cardTagger;

    @Inject
    public DeckAnalysisService(DeckRepository deckRepository, CardService cardService, ClassificationService classificationService) {
        this(deckRepository, cardService, classificationService, new ComboDetectionService(), new CardTagger());
    }

    public DeckAnalysisService(
            DeckRepository deckRepository,
            CardService cardService,
            ClassificationService classificationService,
            ComboDetectionService comboDetectionService
    ) {
        this(deckRepository, cardService, classificationService, comboDetectionService, new CardTagger());
    }

    public DeckAnalysisService(
            DeckRepository deckRepository,
            CardService cardService,
            ClassificationService classificationService,
            ComboDetectionService comboDetectionService,
            CardTagger cardTagger
    ) {
        this.deckRepository = deckRepository;
        this.cardService = cardService;
        this.classificationService = classificationService;
        this.comboDetectionService = comboDetectionService;
        this.cardTagger = cardTagger;
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
        List<DeckCard> cards = mainDeckCards(deck);
        LOG.debugv("analysis.cardCount deckId={0} count={1}", id, cards.size());

        int totalCards = cards.stream().mapToInt(DeckCard::getQuantity).sum();
        double cmcSum = 0.0;
        int rampCount = 0;
        int drawCount = 0;
        int removalCount = 0;
        int landCount = 0;
        int tappedLandCount = 0;
        int boardWipeCount = 0;
        int protectionCount = 0;
        int winconCount = 0;
        int earlyGameCount = 0;
        Map<Integer, Integer> manaCurve = new HashMap<>();
        Map<String, Integer> roles = new LinkedHashMap<>();
        Map<String, Map<Integer, Integer>> manaCurveByType = new LinkedHashMap<>();
        Map<String, Integer> colorCosts = emptyColorMap();
        Map<String, Integer> colorSources = emptyColorMap();
        Map<String, Integer> pipDemand = emptyColorMap();
        Map<String, Integer> cardTags = new LinkedHashMap<>();
        Set<String> deckNames = new HashSet<>();
        int fixingSourceCount = 0;
        int treasureSourceCount = 0;
        int manaRockCount = 0;
        int fetchLandCount = 0;
        int conditionalSourceCount = 0;

        Map<String, CardResponseDTO> cardDetailsByName = cardService.findByNames(cards.stream()
                .map(DeckCard::getName)
                .filter(Objects::nonNull)
                .toList());

        for (DeckCard dc : cards) {
            String name = dc.getName();
            int qty = dc.getQuantity();
            deckNames.add(name);

            CardResponseDTO card = cardDetailsByName.get(cardService.normalizeLookupName(name));

            double cmc = card != null && card.cmc() != null ? card.cmc() : 0.0;
            String oracle = card != null ? nullToEmpty(card.oracleText()).toLowerCase() : "";
            String type = card != null ? nullToEmpty(card.typeLine()).toLowerCase() : "";
            String manaCost = card != null ? nullToEmpty(card.manaCost()) : "";

            cmcSum += cmc * qty;

            int cmcKey = (int) Math.round(cmc);
            manaCurve.merge(cmcKey, qty, Integer::sum);
            manaCurveByType.computeIfAbsent(primaryType(type), ignored -> new HashMap<>()).merge(cmcKey, qty, Integer::sum);
            addColorCosts(colorCosts, manaCost, qty);
            addPipDemand(pipDemand, manaCost, qty);
            addColorSources(colorSources, card, qty);
            Set<String> tags = cardTagger.tagCard(card);
            tags.forEach(tag -> cardTags.merge(tag, qty, Integer::sum));
            if (tags.contains("fixing")) fixingSourceCount += qty;
            if (tags.contains("treasure")) treasureSourceCount += qty;
            if (tags.contains("mana-rock")) manaRockCount += qty;
            if (tags.contains("fetch-land")) fetchLandCount += qty;
            if (isConditionalManaSource(oracle)) conditionalSourceCount += qty;

            if (cmc <= 2.0 && !type.contains("land")) earlyGameCount += qty;
            if (type.contains("land")) {
                landCount += qty;
                if (entersTapped(oracle)) tappedLandCount += qty;
            }
            if (isBoardWipe(oracle)) boardWipeCount += qty;
            if (isProtection(oracle)) protectionCount += qty;
            if (isWincon(type, oracle, cmc)) winconCount += qty;

            ClassificationService.CardCategory cat = classificationService.classify(card != null ? card.oracleText() : null);
            switch (cat) {
                case RAMP -> {
                    rampCount += qty;
                    roles.merge("ramp", qty, Integer::sum);
                }
                case DRAW -> {
                    drawCount += qty;
                    roles.merge("draw", qty, Integer::sum);
                }
                case REMOVAL -> {
                    removalCount += qty;
                    roles.merge("interaction", qty, Integer::sum);
                }
                default -> {}
            }
            if (isProtection(oracle)) roles.merge("protection", qty, Integer::sum);
            if (isBoardWipe(oracle)) roles.merge("boardWipe", qty, Integer::sum);
            if (isWincon(type, oracle, cmc)) roles.merge("wincon", qty, Integer::sum);
            if (type.contains("land")) roles.merge("land", qty, Integer::sum);
        }

        double averageCmc = totalCards > 0 ? cmcSum / (double) totalCards : 0.0;
        int untappedLandCount = Math.max(0, landCount - tappedLandCount);
        ManaBaseAnalysis manaBase = new ManaBaseAnalysis(
                colorCosts,
                colorSources,
                landCount,
                tappedLandCount,
                Map.of(1, untappedLandCount, 2, untappedLandCount + cheapManaSources(cards, cardDetailsByName, 2), 3, untappedLandCount + cheapManaSources(cards, cardDetailsByName, 3)),
                pipDemand,
                fixingSourceCount,
                treasureSourceCount,
                manaRockCount,
                fetchLandCount,
                conditionalSourceCount
        );
        ProbabilityAnalysis probabilities = new ProbabilityAnalysis(
                probabilityAtLeast(totalCards, landCount, 7, 2),
                probabilityAtLeast(totalCards, rampCount, 9, 1),
                probabilityAtLeast(totalCards, removalCount, 10, 1)
        );
        ComboAnalysis combos = comboDetectionService.analyze(deckNames);
        ExplainableScore score = score(
                totalCards,
                averageCmc,
                rampCount,
                drawCount,
                removalCount,
                protectionCount,
                winconCount,
                combos,
                probabilities
        );

        DeckAnalysis analysis = new DeckAnalysis(
                averageCmc,
                totalCards,
                rampCount,
                drawCount,
                removalCount,
                manaCurve,
                roles,
                manaBase,
                manaCurveByType,
                earlyGameCount,
                removalCount,
                boardWipeCount,
                protectionCount,
                winconCount,
                combos,
                probabilities,
                score,
                cardTags
        );
        LOG.debugv("analysis.result deckId={0} {1}", id, analysis);
        return analysis;
    }

    private Map<String, Integer> emptyColorMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        COLORS.forEach(color -> map.put(color, 0));
        return map;
    }

    private void addColorCosts(Map<String, Integer> costs, String manaCost, int quantity) {
        Matcher matcher = MANA_SYMBOL.matcher(manaCost);
        while (matcher.find()) {
            costs.merge(matcher.group(1), quantity, Integer::sum);
        }
    }

    private void addPipDemand(Map<String, Integer> demand, String manaCost, int quantity) {
        Matcher matcher = MANA_SYMBOL.matcher(manaCost);
        while (matcher.find()) {
            String symbol = matcher.group(1);
            if (!"C".equals(symbol)) {
                demand.merge(symbol, quantity, Integer::sum);
            }
        }
    }

    private void addColorSources(Map<String, Integer> sources, CardResponseDTO card, int quantity) {
        if (card == null) return;
        String oracle = nullToEmpty(card.oracleText()).toLowerCase();
        String type = nullToEmpty(card.typeLine()).toLowerCase();
        for (String color : COLORS) {
            if (oracle.contains("{" + color.toLowerCase() + "}") || producesByType(type, color)) {
                sources.merge(color, quantity, Integer::sum);
            }
        }
        if (oracle.contains("any color")) {
            COLORS.stream().filter(color -> !"C".equals(color)).forEach(color -> sources.merge(color, quantity, Integer::sum));
        }
    }

    private boolean producesByType(String type, String color) {
        return switch (color) {
            case "W" -> type.contains("plains");
            case "U" -> type.contains("island");
            case "B" -> type.contains("swamp");
            case "R" -> type.contains("mountain");
            case "G" -> type.contains("forest");
            default -> type.contains("wastes");
        };
    }

    private String primaryType(String type) {
        if (type.contains("land")) return "land";
        if (type.contains("creature")) return "creature";
        if (type.contains("artifact")) return "artifact";
        if (type.contains("enchantment")) return "enchantment";
        if (type.contains("instant")) return "instant";
        if (type.contains("sorcery")) return "sorcery";
        if (type.contains("planeswalker")) return "planeswalker";
        return "other";
    }

    private boolean entersTapped(String oracle) {
        return oracle.contains("enters tapped") || oracle.contains("enters the battlefield tapped");
    }

    private boolean isConditionalManaSource(String oracle) {
        return oracle.contains("could produce")
                || oracle.contains("commander's color identity")
                || oracle.contains("chosen color")
                || oracle.contains("any color that a land");
    }

    private boolean isProtection(String oracle) {
        return oracle.contains("hexproof") || oracle.contains("indestructible") || oracle.contains("phase out") || oracle.contains("protection");
    }

    private boolean isBoardWipe(String oracle) {
        return oracle.contains("destroy all") || oracle.contains("exile all") || oracle.contains("all creatures") || oracle.contains("each creature");
    }

    private boolean isWincon(String type, String oracle, double cmc) {
        return oracle.contains("win the game")
                || oracle.contains("loses the game")
                || oracle.contains("infinite")
                || (type.contains("creature") && cmc >= 5.0 && (oracle.contains("double") || oracle.contains("trample") || oracle.contains("damage")));
    }

    private int cheapManaSources(List<DeckCard> cards, Map<String, CardResponseDTO> cardDetailsByName, int maxCmc) {
        int count = 0;
        for (DeckCard deckCard : cards) {
            CardResponseDTO card = cardDetailsByName.get(cardService.normalizeLookupName(deckCard.getName()));
            if (card == null || card.cmc() == null || card.cmc() > maxCmc) continue;
            String type = nullToEmpty(card.typeLine()).toLowerCase();
            String oracle = nullToEmpty(card.oracleText()).toLowerCase();
            if (!type.contains("land") && (oracle.contains("add ") || oracle.contains("search your library for a land"))) {
                count += deckCard.getQuantity();
            }
        }
        return count;
    }

    private double probabilityAtLeast(int population, int successes, int draws, int atLeast) {
        if (population <= 0 || successes <= 0 || draws <= 0) return 0.0;
        draws = Math.min(draws, population);
        double probability = 0.0;
        int max = Math.min(successes, draws);
        for (int k = atLeast; k <= max; k++) {
            probability += combination(successes, k) * combination(population - successes, draws - k) / combination(population, draws);
        }
        return Math.max(0.0, Math.min(1.0, probability));
    }

    private double combination(int n, int k) {
        if (k < 0 || k > n) return 0.0;
        if (k == 0 || k == n) return 1.0;
        k = Math.min(k, n - k);
        double result = 1.0;
        for (int i = 1; i <= k; i++) {
            result = result * (n - k + i) / i;
        }
        return result;
    }

    private ExplainableScore score(
            int totalCards,
            double averageCmc,
            int ramp,
            int draw,
            int interaction,
            int protection,
            int wincons,
            ComboAnalysis combos,
            ProbabilityAnalysis probabilities
    ) {
        int speed = clampScore((int) Math.round((4.2 - averageCmc) * 18 + ramp * 2.2));
        int consistency = clampScore((int) Math.round(draw * 5.0 + probabilities.openingHandTwoPlusLands() * 35.0));
        int interactionScore = clampScore(interaction * 7);
        int resilience = clampScore(protection * 12 + draw * 2);
        int threat = clampScore(wincons * 12 + combos.present().size() * 20 + combos.oneCardAway().size() * 6);
        int bracketPressure = clampScore((speed + interactionScore + threat + consistency) / 4);
        String summary = totalCards < 90
                ? "Deck incompleto: score reduzido ate a lista se aproximar de 99 cartas."
                : "Score composto por velocidade, consistencia, interacao, resiliencia, ameacas e pressao de bracket.";
        return new ExplainableScore(speed, consistency, interactionScore, resilience, threat, bracketPressure, summary);
    }

    private int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<DeckCard> mainDeckCards(Deck deck) {
        return deck.getCards();
    }
}
