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
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import com.mtg.service.rules.CommanderBanlistService;
import com.mtg.service.rules.CommanderBracketService;
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
        BracketEstimateDTO bracket = commanderBracketService.estimate(mainDeckSize, legal, averageCmc, interaction, ramp);

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
}
