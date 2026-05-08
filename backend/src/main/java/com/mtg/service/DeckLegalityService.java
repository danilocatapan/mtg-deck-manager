package com.mtg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mtg.dto.BanlistStatusDTO;
import com.mtg.dto.BracketEstimateDTO;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.CommanderDTO;
import com.mtg.dto.CompanionStatusDTO;
import com.mtg.dto.DeckLegalityDTO;
import com.mtg.dto.RulesSnapshotDTO;
import com.mtg.domain.ComboAnalysis;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import com.mtg.service.rules.CommanderBanlistService;
import com.mtg.service.rules.CommanderBracketService;
import com.mtg.service.rules.CommanderBracketService.BracketEstimateInput;
import com.mtg.service.rules.CommanderGameChangerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class DeckLegalityService {

    private static final Logger LOG = Logger.getLogger(DeckLegalityService.class);
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private static final int COMMANDER_MAIN_DECK_SIZE = 99;
    private static final Set<String> BASIC_LANDS = Set.of("plains", "island", "swamp", "mountain", "forest", "wastes");
    private static final Set<String> SINGLETON_EXCEPTIONS = Set.of(
            "relentless rats",
            "rat colony",
            "shadowborn apostle",
            "persistent petitioners",
            "nazgul",
            "dragon's approach",
            "seven dwarves",
            "templar knight",
            "hare apparent"
    );

    @Inject
    DeckRepository deckRepository;

    @Inject
    CardService cardService;

    @Inject
    CommanderBanlistService commanderBanlistService;

    @Inject
    CommanderGameChangerService commanderGameChangerService;

    @Inject
    ComboDetectionService comboDetectionService;

    @Inject
    CommanderBracketService commanderBracketService;

    public DeckLegalityDTO check(Long deckId, String ownerId) {
        Deck deck = deckRepository.findByIdAndOwner(deckId, ownerId);
        if (deck == null) {
            throw new NotFoundException("Deck not found");
        }

        List<CommanderDTO> commanders = commandersFor(deck);
        List<String> lookupNames = new ArrayList<>();
        commanders.stream().map(CommanderDTO::name).forEach(lookupNames::add);
        deck.getCards().stream().map(DeckCard::getName).forEach(lookupNames::add);
        Map<String, CardResponseDTO> knownCards = cardService.findByNames(lookupNames);

        List<DeckCard> mainDeckCards = mainDeckCards(deck);
        int mainDeckSize = mainDeckCards.stream().mapToInt(DeckCard::getQuantity).sum();
        boolean sizeLegal = mainDeckSize == COMMANDER_MAIN_DECK_SIZE;

        List<String> duplicateCards = duplicateCards(deck);
        boolean singletonLegal = duplicateCards.isEmpty();

        Set<String> commanderColors = commanderColors(deck, commanders, knownCards);
        List<String> offColorCards = offColorCards(deck, knownCards, commanderColors);
        boolean colorIdentityLegal = offColorCards.isEmpty();

        List<String> bannedCards = bannedCards(deck, commanders);
        BanlistStatusDTO banlist = new BanlistStatusDTO(bannedCards.isEmpty(), bannedCards);

        boolean commanderValid = commanders.stream().allMatch(commander -> isCommanderValid(commander, knownCards.get(normalize(commander.name()))));
        CompanionStatusDTO companion = companionStatus(deck, knownCards);

        double averageCmc = averageCmc(deck, knownCards);
        int interaction = interactionCount(deck, knownCards);
        int ramp = rampCount(deck, knownCards);
        boolean legal = sizeLegal && singletonLegal && colorIdentityLegal && banlist.legal() && commanderValid && companion.legal();
        List<String> gameChangers = gameChangers(deck, commanders);
        BracketSignals signals = bracketSignals(deck, knownCards);
        int comboDensity = comboDensity(deck);
        BracketEstimateDTO bracket = commanderBracketService.estimate(new BracketEstimateInput(
                mainDeckSize,
                legal,
                averageCmc,
                interaction,
                ramp,
                gameChangers.size(),
                comboDensity,
                signals.tutors(),
                signals.fastMana(),
                signals.extraTurns(),
                signals.massLandDestruction(),
                estimateWinTurn(averageCmc, ramp, signals.fastMana(), signals.tutors(), comboDensity),
                null
        ));
        RulesSnapshotDTO rulesSnapshot = rulesSnapshot();

        return new DeckLegalityDTO(
                deck.getId(),
                mainDeckSize,
                COMMANDER_MAIN_DECK_SIZE,
                sizeLegal,
                singletonLegal,
                duplicateCards,
                List.copyOf(commanderColors),
                colorIdentityLegal,
                offColorCards,
                banlist,
                commanderValid,
                commanders,
                companion,
                bracket,
                gameChangers,
                gameChangers.size(),
                rulesSnapshot,
                legal
        );
    }

    private List<CommanderDTO> commandersFor(Deck deck) {
        if (deck.getCommandersJson() != null && !deck.getCommandersJson().isBlank()) {
            try {
                return MAPPER.readValue(deck.getCommandersJson(), new TypeReference<List<CommanderDTO>>() {});
            } catch (Exception exception) {
                LOG.warnv(exception, "event=deck.legality.commanders_json.invalid deckId={0}", deck.getId());
            }
        }
        return List.of(new CommanderDTO(deck.getCommander(), "commander"));
    }

    private List<String> duplicateCards(Deck deck) {
        return mainDeckCards(deck).stream()
                .filter(card -> card.getQuantity() > 1)
                .filter(card -> !isSingletonExempt(card.getName()))
                .map(DeckCard::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private boolean isSingletonExempt(String name) {
        String normalized = normalize(name);
        return BASIC_LANDS.contains(normalized) || SINGLETON_EXCEPTIONS.contains(normalized);
    }

    private Set<String> commanderColors(Deck deck, List<CommanderDTO> commanders, Map<String, CardResponseDTO> knownCards) {
        LinkedHashSet<String> colors = new LinkedHashSet<>();
        for (CommanderDTO commander : commanders) {
            CardResponseDTO card = knownCards.get(normalize(commander.name()));
            if (card != null && card.colorIdentity() != null) {
                colors.addAll(card.colorIdentity());
            }
        }
        if (colors.isEmpty() && deck.getColorIdentity() != null) {
            String normalized = deck.getColorIdentity().replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
            for (char symbol : normalized.toCharArray()) {
                colors.add(String.valueOf(symbol));
            }
        }
        return colors.stream()
                .map(color -> color == null ? "" : color.trim().toUpperCase(Locale.ROOT))
                .filter(color -> Set.of("W", "U", "B", "R", "G", "C").contains(color))
                .sorted(Comparator.comparingInt(this::colorSort))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> offColorCards(Deck deck, Map<String, CardResponseDTO> knownCards, Set<String> commanderColors) {
        return mainDeckCards(deck).stream()
                .filter(card -> {
                    CardResponseDTO info = knownCards.get(normalize(card.getName()));
                    return info != null && !ColorIdentityMatcher.matches(info, commanderColors);
                })
                .map(DeckCard::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<String> bannedCards(Deck deck, List<CommanderDTO> commanders) {
        LinkedHashSet<String> banned = new LinkedHashSet<>();
        for (CommanderDTO commander : commanders) {
            if (commanderBanlistService.isBanned(commander.name())) {
                banned.add(commander.name());
            }
        }
        for (DeckCard card : mainDeckCards(deck)) {
            if (commanderBanlistService.isBanned(card.getName())) {
                banned.add(card.getName());
            }
        }
        for (DeckCard card : companionCards(deck)) {
            if (commanderBanlistService.isBanned(card.getName())) {
                banned.add(card.getName());
            }
        }
        return banned.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private boolean isCommanderValid(CommanderDTO commander, CardResponseDTO card) {
        if (commander == null || commander.name() == null || commander.name().isBlank() || card == null) {
            return false;
        }
        String type = text(card.typeLine());
        String oracle = text(card.oracleText());
        String role = commander.role() == null ? "commander" : commander.role().toLowerCase(Locale.ROOT);
        if ("background".equals(role)) {
            return type.contains("background") || oracle.contains("choose a background");
        }
        return oracle.contains("can be your commander")
                || (type.contains("legendary") && type.contains("creature"));
    }

    private CompanionStatusDTO companionStatus(Deck deck, Map<String, CardResponseDTO> knownCards) {
        List<DeckCard> companions = companionCards(deck);
        if (!companions.isEmpty()) {
            DeckCard companion = companions.getFirst();
            return new CompanionStatusDTO(true, companion.getName(), companions.size() == 1,
                    companions.size() == 1 ? "Companion declarado fora do deck principal." : "Apenas um companion pode ser declarado.");
        }
        for (DeckCard deckCard : mainDeckCards(deck)) {
            CardResponseDTO card = knownCards.get(normalize(deckCard.getName()));
            if (card == null) {
                continue;
            }
            if (text(card.oracleText()).contains("companion")) {
                return new CompanionStatusDTO(
                        true,
                        card.name(),
                        false,
                        "Companion detectado na lista principal; ainda nao ha zona companion dedicada neste contrato."
                );
            }
        }
        return new CompanionStatusDTO(false, null, true, "Nenhum companion declarado.");
    }

    private double averageCmc(Deck deck, Map<String, CardResponseDTO> knownCards) {
        int total = 0;
        double cmc = 0.0;
        for (DeckCard card : mainDeckCards(deck)) {
            CardResponseDTO info = knownCards.get(normalize(card.getName()));
            if (info == null) {
                continue;
            }
            int quantity = card.getQuantity();
            total += quantity;
            cmc += (info.cmc() == null ? 0.0 : info.cmc()) * quantity;
        }
        return total == 0 ? 0.0 : cmc / total;
    }

    private int interactionCount(Deck deck, Map<String, CardResponseDTO> knownCards) {
        return roleCount(deck, knownCards, Set.of("destroy", "exile", "counter target", "return target"));
    }

    private int rampCount(Deck deck, Map<String, CardResponseDTO> knownCards) {
        return roleCount(deck, knownCards, Set.of("add ", "search your library for a land"));
    }

    private List<String> gameChangers(Deck deck, List<CommanderDTO> commanders) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        commanders.stream()
                .map(CommanderDTO::name)
                .filter(commanderGameChangerService::isGameChanger)
                .forEach(names::add);
        mainDeckCards(deck).stream()
                .map(DeckCard::getName)
                .filter(commanderGameChangerService::isGameChanger)
                .forEach(names::add);
        companionCards(deck).stream()
                .map(DeckCard::getName)
                .filter(commanderGameChangerService::isGameChanger)
                .forEach(names::add);
        return names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private BracketSignals bracketSignals(Deck deck, Map<String, CardResponseDTO> knownCards) {
        int tutors = 0;
        int fastMana = 0;
        int extraTurns = 0;
        int massLandDestruction = 0;
        for (DeckCard card : mainDeckCards(deck)) {
            CardResponseDTO info = knownCards.get(normalize(card.getName()));
            if (info == null) {
                continue;
            }
            int quantity = card.getQuantity();
            String oracle = text(info.oracleText());
            String type = text(info.typeLine());
            double cmc = info.cmc() == null ? 0.0 : info.cmc();
            if (isTutor(oracle)) tutors += quantity;
            if (isFastMana(card.getName(), oracle, type, cmc)) fastMana += quantity;
            if (oracle.contains("extra turn")) extraTurns += quantity;
            if (isMassLandDestruction(oracle)) massLandDestruction += quantity;
        }
        return new BracketSignals(tutors, fastMana, extraTurns, massLandDestruction);
    }

    private int comboDensity(Deck deck) {
        if (comboDetectionService == null) {
            return 0;
        }
        Set<String> names = mainDeckCards(deck).stream()
                .map(DeckCard::getName)
                .collect(Collectors.toSet());
        ComboAnalysis combos = comboDetectionService.analyze(names);
        return combos.present().size() + combos.oneCardAway().size();
    }

    private boolean isTutor(String oracle) {
        return oracle.contains("search your library")
                && !oracle.contains("basic land")
                && !oracle.contains("land card")
                && !oracle.contains("forest card")
                && !oracle.contains("plains, island, swamp, or mountain");
    }

    private boolean isFastMana(String name, String oracle, String type, double cmc) {
        String normalized = normalize(name);
        if (Set.of("sol ring", "mana vault", "grim monolith", "chrome mox", "mox diamond", "lion's eye diamond").contains(normalized)) {
            return true;
        }
        return type.contains("artifact")
                && cmc <= 1.0
                && oracle.contains("add ")
                && !oracle.contains("enters tapped");
    }

    private boolean isMassLandDestruction(String oracle) {
        return oracle.contains("destroy all lands")
                || oracle.contains("destroy all nonbasic lands")
                || oracle.contains("each player sacrifices all lands")
                || oracle.contains("exile all lands");
    }

    private int estimateWinTurn(double averageCmc, int ramp, int fastMana, int tutors, int comboDensity) {
        int estimate = 9;
        if (comboDensity > 0) estimate -= 2;
        if (comboDensity >= 2) estimate -= 1;
        if (fastMana >= 2) estimate -= 1;
        if (tutors >= 2) estimate -= 1;
        if (ramp >= 12) estimate -= 1;
        if (averageCmc <= 2.6) estimate -= 1;
        return Math.max(3, estimate);
    }

    private RulesSnapshotDTO rulesSnapshot() {
        CommanderBanlistService.CommanderBanlistSnapshot banlistSnapshot = commanderBanlistService.load();
        CommanderGameChangerService.GameChangerSnapshot gameChangerSnapshot = commanderGameChangerService.load();
        return new RulesSnapshotDTO(
                banlistSnapshot.effectiveDate(),
                gameChangerSnapshot.effectiveDate(),
                gameChangerSnapshot.bracketVersion(),
                "scryfall-cache-current",
                List.of("banlist", "gameChangers", "colorIdentity", "singleton", "companion", "commanderValidity")
        );
    }

    private int roleCount(Deck deck, Map<String, CardResponseDTO> knownCards, Set<String> needles) {
        int count = 0;
        for (DeckCard card : mainDeckCards(deck)) {
            CardResponseDTO info = knownCards.get(normalize(card.getName()));
            if (info == null) {
                continue;
            }
            String oracle = text(info.oracleText());
            if (needles.stream().anyMatch(oracle::contains)) {
                count += card.getQuantity();
            }
        }
        return count;
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private int colorSort(String color) {
        return switch (color) {
            case "W" -> 0;
            case "U" -> 1;
            case "B" -> 2;
            case "R" -> 3;
            case "G" -> 4;
            case "C" -> 5;
            default -> 6;
        };
    }

    private List<DeckCard> mainDeckCards(Deck deck) {
        return deck.getCards().stream()
                .filter(card -> "main".equals(card.getZone()))
                .toList();
    }

    private List<DeckCard> companionCards(Deck deck) {
        return deck.getCards().stream()
                .filter(card -> "companion".equals(card.getZone()))
                .toList();
    }

    private record BracketSignals(int tutors, int fastMana, int extraTurns, int massLandDestruction) {
    }
}
