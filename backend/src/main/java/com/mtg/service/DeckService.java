package com.mtg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mtg.dto.ApplyRecommendationSwapDTO;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.CommanderDTO;
import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.DeckHistoryEntryDTO;
import com.mtg.dto.DeckPackageDTO;
import com.mtg.dto.DeckRequestDTO;
import com.mtg.dto.DeckResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import com.mtg.dto.DeckImportDTO;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;

@ApplicationScoped
public class DeckService {

    private static final Logger LOG = Logger.getLogger(DeckService.class);
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private static final int MAX_DECK_NAME_LENGTH = 120;
    private static final int MAX_CARD_NAME_LENGTH = 120;
    private static final int MAX_DECK_CARDS = 120;
    private static final int MAX_COMMANDER_MAIN_DECK_CARDS = 99;
    private static final Set<String> COMMANDER_ROLES = Set.of("commander", "background", "partner");
    private static final Set<String> CARD_ZONES = Set.of("main", "maybeboard", "considering", "companion");
    private static final Set<String> BASIC_LANDS = Set.of(
            "plains",
            "island",
            "swamp",
            "mountain",
            "forest",
            "wastes"
    );

    private final DeckRepository deckRepository;

    @Inject
    DeckImportService importService;

    @Inject
    CardService cardService;

    @Inject
    public DeckService(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    @Transactional
    public DeckResponseDTO createDeck(DeckRequestDTO request, String ownerId) {
        validateRequest(request);
        validateOwner(ownerId);
        LOG.debug("Creating deck: " + request);
        List<CommanderDTO> commanders = normalizeCommanders(request.commander(), request.commanders());
        Map<String, CardResponseDTO> resolved = validateCardsExist(commanders, request.cards());

        List<DeckCard> cards = toEntities(request.cards());
        Deck deck = new Deck(request.name().trim(), primaryCommander(commanders), cards);
        deck.setOwnerId(ownerId);
        deck.setCommandersJson(toCommandersJson(commanders));
        deck.setColorIdentity(toColorIdentity(commanders, resolved));
        deckRepository.persist(deck);
        LOG.info("Deck created: " + deck.getId());

        return toDto(deck);
    }

    @Transactional
    public DeckResponseDTO importDeck(DeckImportDTO dto, String ownerId) {
        validateOwner(ownerId);
        if (dto == null) throw new IllegalArgumentException("Import payload required");
        validateDeckName(dto.name());
        validateCommander(dto.commander());
        if (dto.content() != null && dto.content().length() > 16_000) {
            throw new IllegalArgumentException("Import payload is too large");
        }
        List<CommanderDTO> commanders = normalizeCommanders(dto.commander(), dto.commanders());

        var cards = removeCommandersFromMainDeck(importService.parse(dto.content()), commanders);
        int total = cards.stream().mapToInt(DeckCard::getQuantity).sum();
        if (total > 99) {
            throw new IllegalArgumentException("Imported deck has " + total + " cards; maximum is 99.");
        }
        Map<String, CardResponseDTO> resolved = validateCardsExist(commanders, cards.stream()
                .map(card -> new DeckCardDTO(card.getName(), card.getQuantity()))
                .toList());

        Deck deck = new Deck();
        deck.setName(dto.name().trim());
        deck.setCommander(primaryCommander(commanders));
        deck.setOwnerId(ownerId);
        deck.setCommandersJson(toCommandersJson(commanders));
        deck.setColorIdentity(toColorIdentity(commanders, resolved));
        deck.setCards(cards);
        deckRepository.persist(deck);
        return toDto(deck);
    }

    public List<DeckResponseDTO> listDecks(String ownerId) {
        validateOwner(ownerId);
        return deckRepository.listByOwner(ownerId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public DeckResponseDTO getDeckById(Long id, String ownerId) {
        validateOwner(ownerId);
        Deck deck = deckRepository.findByIdAndOwner(id, ownerId);
        if (deck == null) {
            return null;
        }
        return toDto(deck);
    }

    @Transactional
    public DeckResponseDTO updateDeck(Long id, DeckRequestDTO request, String ownerId) {
        validateRequest(request);
        validateOwner(ownerId);
        Deck deck = deckRepository.findByIdAndOwner(id, ownerId);
        if (deck == null) {
            return null;
        }
        LOG.debug("Updating deck: " + id);
        List<CommanderDTO> commanders = normalizeCommanders(request.commander(), request.commanders());
        Map<String, CardResponseDTO> resolved = validateCardsExist(commanders, request.cards());
        deck.setName(request.name().trim());
        deck.setCommander(primaryCommander(commanders));
        deck.setCommandersJson(toCommandersJson(commanders));
        deck.setColorIdentity(toColorIdentity(commanders, resolved));
        deck.setCards(toEntities(request.cards()));
        deckRepository.persist(deck);
        LOG.info("Deck updated: " + id);
        return toDto(deck);
    }

    @Transactional
    public boolean deleteDeck(Long id, String ownerId) {
        validateOwner(ownerId);
        LOG.info("Deleting deck: " + id);
        return deckRepository.delete("id = ?1 and ownerId = ?2", id, ownerId) > 0;
    }

    @Transactional
    public DeckResponseDTO applyRecommendationSwap(Long deckId, ApplyRecommendationSwapDTO dto, String ownerId) {
        validateOwner(ownerId);
        validateSwap(deckId, dto);

        Deck deck = deckRepository.findByIdAndOwner(deckId, ownerId);
        if (deck == null) {
            return null;
        }

        int totalBefore = totalCards(deck);
        if (totalBefore > MAX_COMMANDER_MAIN_DECK_CARDS) {
            logInvalidSwap(deckId, "deck_has_more_than_99_cards");
            throw new IllegalArgumentException("Deck main deck must have at most 99 cards");
        }

        String add = dto.add().trim();
        String remove = dto.remove().trim();
        validateCardExists(add, "Card to add was not found");
        DeckCard removeCard = findMainDeckCard(deck, remove);
        if (removeCard == null) {
            logInvalidSwap(deckId, "remove_card_not_found");
            throw new IllegalArgumentException("Card to remove was not found in deck");
        }

        DeckCard existingAdd = findMainDeckCard(deck, add);
        if (existingAdd != null && !isBasicLand(add)) {
            logInvalidSwap(deckId, "add_card_already_exists");
            throw new IllegalArgumentException("Card to add already exists in deck");
        }

        removeOne(deck, removeCard);
        addOne(deck, add, existingAdd);
        appendHistory(deck, dto);

        int totalAfter = totalCards(deck);
        if (totalAfter != totalBefore) {
            logInvalidSwap(deckId, "card_total_changed");
            throw new IllegalArgumentException("Swap must preserve deck card count");
        }
        if (totalAfter > MAX_COMMANDER_MAIN_DECK_CARDS) {
            logInvalidSwap(deckId, "deck_exceeds_99_cards");
            throw new IllegalArgumentException("Deck main deck must have at most 99 cards");
        }

        deckRepository.persist(deck);
        LOG.infov("event=recommendation.swap.applied deckId={0} add=\"{1}\" remove=\"{2}\"", deckId, add, remove);
        return toDto(deck);
    }

    @Transactional
    public DeckResponseDTO undoRecommendationSwap(Long deckId, String historyId, String ownerId) {
        validateOwner(ownerId);
        Deck deck = deckRepository.findByIdAndOwner(deckId, ownerId);
        if (deck == null) {
            return null;
        }

        List<DeckHistoryEntryDTO> history = new ArrayList<>(historyFor(deck));
        DeckHistoryEntryDTO entry = findUndoEntry(history, historyId);
        if (entry == null) {
            throw new IllegalArgumentException("Swap history entry was not found");
        }

        DeckCard addedCard = findMainDeckCard(deck, entry.add());
        if (addedCard == null) {
            throw new IllegalArgumentException("Card added by the swap is no longer in the main deck");
        }
        DeckCard removedCard = findMainDeckCard(deck, entry.remove());
        if (removedCard != null && !isBasicLand(entry.remove())) {
            throw new IllegalArgumentException("Card removed by the swap is already back in the main deck");
        }

        removeOne(deck, addedCard);
        addOne(deck, entry.remove(), removedCard);
        replaceHistory(deck, markUndone(history, entry.id()));
        deckRepository.persist(deck);
        LOG.infov("event=recommendation.swap.undone deckId={0} add=\"{1}\" remove=\"{2}\"", deckId, entry.add(), entry.remove());
        return toDto(deck);
    }

    public List<DeckPackageDTO> recommendPackages(Long deckId, String ownerId) {
        validateOwner(ownerId);
        Deck deck = deckRepository.findByIdAndOwner(deckId, ownerId);
        if (deck == null) {
            return null;
        }
        String colors = deck.getColorIdentity() == null ? "" : deck.getColorIdentity();
        List<DeckPackageDTO> packages = new ArrayList<>();
        packages.add(new DeckPackageDTO(
                "protecao",
                "Pacote Protecao",
                "Cartas para proteger comandante e pecas-chave durante uma rodada de mesa.",
                "maybeboard",
                List.of("protection", "preserve-theme"),
                List.of(
                        new DeckCardDTO("Swiftfoot Boots", 1, "maybeboard"),
                        new DeckCardDTO("Lightning Greaves", 1, "maybeboard"),
                        new DeckCardDTO(colors.contains("G") ? "Heroic Intervention" : "Darksteel Plate", 1, "maybeboard")
                )
        ));
        packages.add(new DeckPackageDTO(
                colors.contains("R") && colors.contains("G") ? "ramp-gruul" : "ramp-universal",
                colors.contains("R") && colors.contains("G") ? "Pacote Ramp Gruul" : "Pacote Ramp Universal",
                "Aceleracao simples para melhorar acesso aos turnos 2 e 3.",
                "maybeboard",
                List.of("ramp", "improve-mana"),
                List.of(
                        new DeckCardDTO(colors.contains("G") ? "Nature's Lore" : "Arcane Signet", 1, "maybeboard"),
                        new DeckCardDTO("Fellwar Stone", 1, "maybeboard"),
                        new DeckCardDTO("Wayfarer's Bauble", 1, "maybeboard")
                )
        ));
        packages.add(new DeckPackageDTO(
                "lands-budget",
                "Pacote Lands Budget",
                "Terrenos baratos para ajustar fixing sem aumentar muito o custo da lista.",
                "maybeboard",
                List.of("land", "budget", "fixing"),
                List.of(
                        new DeckCardDTO("Command Tower", 1, "maybeboard"),
                        new DeckCardDTO("Path of Ancestry", 1, "maybeboard"),
                        new DeckCardDTO("Evolving Wilds", 1, "maybeboard")
                )
        ));
        return packages;
    }

    @Transactional
    public DeckResponseDTO addPackageToMaybeboard(Long deckId, String packageId, String ownerId) {
        validateOwner(ownerId);
        Deck deck = deckRepository.findByIdAndOwner(deckId, ownerId);
        if (deck == null) {
            return null;
        }
        DeckPackageDTO selected = recommendPackages(deckId, ownerId).stream()
                .filter(deckPackage -> deckPackage.id().equals(packageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Package was not found"));

        validateCardsExist(commandersFor(deck), selected.cards());
        for (DeckCardDTO card : selected.cards()) {
            DeckCard existing = findCardInZone(deck, card.name(), "maybeboard");
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + card.quantity());
            } else {
                DeckCard added = new DeckCard(card.name(), card.quantity(), "maybeboard");
                added.setDeck(deck);
                deck.getCards().add(added);
            }
        }
        deckRepository.persist(deck);
        return toDto(deck);
    }

    public String exportDeck(Long id, String ownerId) {
        validateOwner(ownerId);
        LOG.info("Export requested: " + id);
        Deck deck = deckRepository.findByIdAndOwner(id, ownerId);
        if (deck == null) {
            LOG.error("Export failed: deck not found " + id);
            return null;
        }
        List<DeckCard> cards = mainDeckCards(deck);
        LOG.debug("Exporting deck " + id + ", card count=" + (cards == null ? 0 : cards.size()));
        if (cards == null || cards.isEmpty()) {
            return "";
        }

        return cards.stream()
                .map(c -> c.getQuantity() + " " + c.getName())
                .collect(Collectors.joining("\n"));
    }

    private void validateRequest(DeckRequestDTO request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Deck name is required");
        }
        validateDeckName(request.name());
        normalizeCommanders(request.commander(), request.commanders());
        if (request.cards() == null || request.cards().isEmpty()) {
            throw new IllegalArgumentException("Deck must contain at least one card");
        }
        if (request.cards().size() > MAX_DECK_CARDS) {
            throw new IllegalArgumentException("Deck accepts at most " + MAX_DECK_CARDS + " card entries");
        }
        request.cards().forEach(this::validateCard);
        int mainDeckTotal = request.cards().stream()
                .filter(card -> "main".equals(normalizeZone(card.zone())))
                .mapToInt(DeckCardDTO::quantity)
                .sum();
        if (mainDeckTotal <= 0) {
            throw new IllegalArgumentException("Deck must contain at least one main deck card");
        }
        if (mainDeckTotal > MAX_COMMANDER_MAIN_DECK_CARDS) {
            throw new IllegalArgumentException("Commander main deck must have at most 99 cards");
        }
    }

    private List<DeckCard> toEntities(List<DeckCardDTO> cards) {
        return cards.stream()
                .map(c -> new DeckCard(c.name().trim(), c.quantity(), normalizeZone(c.zone())))
                .collect(Collectors.toList());
    }

    private List<DeckCard> removeCommandersFromMainDeck(List<DeckCard> cards, List<CommanderDTO> commanders) {
        Set<String> commanderNames = commanders.stream()
                .map(CommanderDTO::name)
                .map(this::normalize)
                .collect(Collectors.toSet());
        return cards.stream()
                .filter(card -> !commanderNames.contains(normalize(card.getName())))
                .toList();
    }

    private void validateDeckName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Deck name is required");
        }
        if (name.length() > MAX_DECK_NAME_LENGTH) {
            throw new IllegalArgumentException("Deck name is too long");
        }
    }

    private void validateCommander(String commander) {
        if (commander == null || commander.isBlank()) {
            throw new IllegalArgumentException("Commander is required");
        }
        if (commander.length() > MAX_CARD_NAME_LENGTH) {
            throw new IllegalArgumentException("Commander name is too long");
        }
    }

    private void validateCard(DeckCardDTO card) {
        if (card == null || card.name() == null || card.name().isBlank()) {
            throw new IllegalArgumentException("Card name is required");
        }
        if (card.name().length() > MAX_CARD_NAME_LENGTH) {
            throw new IllegalArgumentException("Card name is too long");
        }
        if (card.quantity() < 1 || card.quantity() > 99) {
            throw new IllegalArgumentException("Card quantity must be between 1 and 99");
        }
        normalizeZone(card.zone());
    }

    private Map<String, CardResponseDTO> validateCardsExist(List<CommanderDTO> commanders, List<DeckCardDTO> cards) {
        List<String> names = new java.util.ArrayList<>();
        commanders.stream().map(CommanderDTO::name).forEach(names::add);
        cards.stream().map(DeckCardDTO::name).forEach(names::add);

        Map<String, CardResponseDTO> resolved = cardService.findByNames(names);
        for (String name : names) {
            if (!resolved.containsKey(cardService.normalizeLookupName(name))) {
                LOG.warnv("event=deck.card_validation.failed card=\"{0}\"", name);
                throw new IllegalArgumentException("Card not found: " + name.trim());
            }
        }
        return resolved;
    }

    private void validateCardExists(String name, String message) {
        Map<String, CardResponseDTO> resolved = cardService.findByNames(List.of(name));
        if (!resolved.containsKey(cardService.normalizeLookupName(name))) {
            LOG.warnv("event=deck.card_validation.failed card=\"{0}\"", name);
            throw new IllegalArgumentException(message + ": " + name.trim());
        }
    }

    private void validateSwap(Long deckId, ApplyRecommendationSwapDTO dto) {
        if (dto == null) {
            logInvalidSwap(deckId, "payload_required");
            throw new IllegalArgumentException("Swap payload is required");
        }
        if (dto.add() == null || dto.add().isBlank()) {
            logInvalidSwap(deckId, "add_required");
            throw new IllegalArgumentException("Card to add is required");
        }
        if (dto.remove() == null || dto.remove().isBlank()) {
            logInvalidSwap(deckId, "remove_required");
            throw new IllegalArgumentException("Card to remove is required");
        }
        validateSwapCardName(deckId, dto.add(), "add_card_name_too_long", "Card to add name is too long");
        validateSwapCardName(deckId, dto.remove(), "remove_card_name_too_long", "Card to remove name is too long");
        if (normalize(dto.add()).equals(normalize(dto.remove()))) {
            logInvalidSwap(deckId, "same_add_and_remove");
            throw new IllegalArgumentException("Card to add and card to remove must be different");
        }
    }

    private void validateSwapCardName(Long deckId, String name, String reason, String message) {
        if (name.trim().length() > MAX_CARD_NAME_LENGTH) {
            logInvalidSwap(deckId, reason);
            throw new IllegalArgumentException(message);
        }
    }

    private DeckCard findMainDeckCard(Deck deck, String name) {
        String normalized = normalize(name);
        return mainDeckCards(deck).stream()
                .filter(card -> normalize(card.getName()).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private void removeOne(Deck deck, DeckCard card) {
        card.setQuantity(card.getQuantity() - 1);
        if (card.getQuantity() <= 0) {
            deck.getCards().remove(card);
            card.setDeck(null);
        }
    }

    private void addOne(Deck deck, String name, DeckCard existingAdd) {
        if (existingAdd != null) {
            existingAdd.setQuantity(existingAdd.getQuantity() + 1);
            return;
        }
        DeckCard added = new DeckCard(name, 1);
        added.setZone("main");
        added.setDeck(deck);
        deck.getCards().add(added);
    }

    private int totalCards(Deck deck) {
        return mainDeckCards(deck).stream().mapToInt(DeckCard::getQuantity).sum();
    }

    private boolean isBasicLand(String name) {
        return BASIC_LANDS.contains(normalize(name));
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private void logInvalidSwap(Long deckId, String reason) {
        LOG.warnv("event=recommendation.swap.invalid deckId={0} reason={1}", deckId, reason);
    }

    private void validateOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
    }

    private DeckResponseDTO toDto(Deck deck) {
        List<DeckCardDTO> cards = deck.getCards().stream()
                .map(c -> new DeckCardDTO(c.getName(), c.getQuantity(), normalizeZone(c.getZone())))
                .collect(Collectors.toList());
        return new DeckResponseDTO(deck.getId(), deck.getName(), deck.getCommander(), cards, deck.getColorIdentity(), commandersFor(deck), historyFor(deck));
    }

    private List<DeckCard> mainDeckCards(Deck deck) {
        return deck.getCards().stream()
                .filter(card -> "main".equals(normalizeZone(card.getZone())))
                .toList();
    }

    private String normalizeZone(String zone) {
        if (zone == null || zone.isBlank()) {
            return "main";
        }
        String normalized = zone.trim().toLowerCase(Locale.ROOT).replace("_", "-");
        if ("sideboard".equals(normalized) || "maybe".equals(normalized)) {
            normalized = "maybeboard";
        }
        if (!CARD_ZONES.contains(normalized)) {
            throw new IllegalArgumentException("Card zone must be main, maybeboard, considering, or companion");
        }
        return normalized;
    }

    private void appendHistory(Deck deck, ApplyRecommendationSwapDTO dto) {
        List<DeckHistoryEntryDTO> history = new ArrayList<>(historyFor(deck));
        history.add(new DeckHistoryEntryDTO(
                dto.recommendationId() == null || dto.recommendationId().isBlank()
                        ? java.util.UUID.randomUUID().toString()
                        : dto.recommendationId(),
                dto.add().trim(),
                dto.remove().trim(),
                dto.source(),
                dto.confidence(),
                dto.problem(),
                dto.risk(),
                dto.impactSummary(),
                OffsetDateTime.now().toString(),
                false
        ));
        replaceHistory(deck, history);
    }

    private List<DeckHistoryEntryDTO> historyFor(Deck deck) {
        if (deck.getHistoryJson() == null || deck.getHistoryJson().isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(deck.getHistoryJson(), new TypeReference<List<DeckHistoryEntryDTO>>() {});
        } catch (Exception exception) {
            LOG.warnv(exception, "event=deck.history_json.invalid deckId={0}", deck.getId());
            return List.of();
        }
    }

    private void replaceHistory(Deck deck, List<DeckHistoryEntryDTO> history) {
        try {
            deck.setHistoryJson(MAPPER.writeValueAsString(history == null ? List.of() : history));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to serialize deck history");
        }
    }

    private DeckHistoryEntryDTO findUndoEntry(List<DeckHistoryEntryDTO> history, String historyId) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        if (historyId != null && !historyId.isBlank()) {
            return history.stream()
                    .filter(entry -> !entry.undone())
                    .filter(entry -> historyId.equals(entry.id()))
                    .findFirst()
                    .orElse(null);
        }
        for (int index = history.size() - 1; index >= 0; index--) {
            DeckHistoryEntryDTO entry = history.get(index);
            if (!entry.undone()) {
                return entry;
            }
        }
        return null;
    }

    private List<DeckHistoryEntryDTO> markUndone(List<DeckHistoryEntryDTO> history, String id) {
        return history.stream()
                .map(entry -> id.equals(entry.id())
                        ? new DeckHistoryEntryDTO(entry.id(), entry.add(), entry.remove(), entry.source(), entry.confidence(), entry.problem(), entry.risk(), entry.impactSummary(), entry.appliedAt(), true)
                        : entry)
                .toList();
    }

    private DeckCard findCardInZone(Deck deck, String name, String zone) {
        String normalized = normalize(name);
        String normalizedZone = normalizeZone(zone);
        return deck.getCards().stream()
                .filter(card -> normalize(card.getName()).equals(normalized))
                .filter(card -> normalizeZone(card.getZone()).equals(normalizedZone))
                .findFirst()
                .orElse(null);
    }

    private List<CommanderDTO> normalizeCommanders(String legacyCommander, List<CommanderDTO> requestedCommanders) {
        List<CommanderDTO> source = requestedCommanders == null || requestedCommanders.isEmpty()
                ? List.of(new CommanderDTO(legacyCommander, "commander"))
                : requestedCommanders;
        List<CommanderDTO> normalized = new ArrayList<>();

        for (CommanderDTO commander : source) {
            if (commander == null || commander.name() == null || commander.name().isBlank()) {
                throw new IllegalArgumentException("Commander is required");
            }
            validateCommander(commander.name());
            String role = commander.role() == null || commander.role().isBlank()
                    ? "commander"
                    : commander.role().trim().toLowerCase(Locale.ROOT).replace("_", "-");
            if (!COMMANDER_ROLES.contains(role)) {
                throw new IllegalArgumentException("Commander role must be commander, background, or partner");
            }
            normalized.add(new CommanderDTO(commander.name().trim(), role));
        }

        boolean hasMainCommander = normalized.stream().anyMatch(commander -> "commander".equals(commander.role()));
        if (!hasMainCommander) {
            throw new IllegalArgumentException("At least one primary commander is required");
        }
        return normalized.stream()
                .sorted(Comparator.comparing((CommanderDTO commander) -> roleSort(commander.role())).thenComparing(CommanderDTO::name))
                .toList();
    }

    private int roleSort(String role) {
        return switch (role) {
            case "commander" -> 0;
            case "partner" -> 1;
            case "background" -> 2;
            default -> 3;
        };
    }

    private String primaryCommander(List<CommanderDTO> commanders) {
        return commanders.stream()
                .filter(commander -> "commander".equals(commander.role()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("At least one primary commander is required"))
                .name();
    }

    private String toCommandersJson(List<CommanderDTO> commanders) {
        try {
            return MAPPER.writeValueAsString(commanders);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to serialize commanders");
        }
    }

    private List<CommanderDTO> commandersFor(Deck deck) {
        if (deck.getCommandersJson() != null && !deck.getCommandersJson().isBlank()) {
            try {
                return MAPPER.readValue(deck.getCommandersJson(), new TypeReference<List<CommanderDTO>>() {});
            } catch (Exception exception) {
                LOG.warnv(exception, "event=deck.commanders_json.invalid deckId={0}", deck.getId());
            }
        }
        return List.of(new CommanderDTO(deck.getCommander(), "commander"));
    }

    private String toColorIdentity(List<CommanderDTO> commanders, Map<String, CardResponseDTO> resolvedCards) {
        LinkedHashSet<String> colors = new LinkedHashSet<>();
        for (CommanderDTO commander : commanders) {
            CardResponseDTO card = resolvedCards.get(cardService.normalizeLookupName(commander.name()));
            if (card != null && card.colorIdentity() != null) {
                colors.addAll(card.colorIdentity());
            }
        }
        return colors.stream()
                .map(color -> color == null ? "" : color.trim().toUpperCase(Locale.ROOT))
                .filter(color -> Set.of("W", "U", "B", "R", "G", "C").contains(color))
                .sorted(Comparator.comparingInt(this::colorSort))
                .collect(Collectors.joining());
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
}
