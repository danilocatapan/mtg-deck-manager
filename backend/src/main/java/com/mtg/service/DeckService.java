package com.mtg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mtg.dto.ApplyRecommendationSwapDTO;
import com.mtg.dto.AuthenticatedUserDTO;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.CommanderDTO;
import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.DeckConsultResponseDTO;
import com.mtg.dto.DeckHistoryEntryDTO;
import com.mtg.dto.DeckRequestDTO;
import com.mtg.dto.DeckResponseDTO;
import com.mtg.dto.PublicDeckSummaryDTO;
import com.mtg.dto.PublicDeckResponseDTO;
import com.mtg.model.DeckVisibility;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.model.DeckLike;
import com.mtg.repository.DeckLikeRepository;
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
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class DeckService {

    private static final Logger LOG = Logger.getLogger(DeckService.class);
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private static final int MAX_DECK_NAME_LENGTH = 120;
    private static final int MAX_CARD_NAME_LENGTH = 120;
    private static final int MAX_DECK_CARDS = 120;
    private static final int MAX_COMMANDER_MAIN_DECK_CARDS = 99;
    private static final Set<String> COMMANDER_ROLES = Set.of("commander", "background", "partner");
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
    DeckLikeRepository deckLikeRepository;

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
        return createDeck(request, ownerId, null);
    }

    @Transactional
    public DeckResponseDTO createDeck(DeckRequestDTO request, AuthenticatedUserDTO user) {
        validateUser(user);
        return createDeck(request, user.googleSubject(), user.name());
    }

    @Transactional
    public DeckResponseDTO createDeck(DeckRequestDTO request, String ownerId, String authorDisplayName) {
        validateRequest(request);
        validateOwner(ownerId);
        LOG.debug("event=deck.create.request");
        List<CommanderDTO> commanders = normalizeCommanders(request.commander(), request.commanders());
        Map<String, CardResponseDTO> resolved = validateCardsExist(commanders, request.cards());

        List<DeckCard> cards = toEntities(request.cards());
        Deck deck = new Deck(request.name().trim(), primaryCommander(commanders), cards);
        deck.setOwnerId(ownerId);
        deck.setAuthorDisplayName(toPublicAuthor(authorDisplayName));
        deck.setVisibility(normalizeVisibility(request == null ? null : request.visibility()));
        deck.setCommandersJson(toCommandersJson(commanders));
        deck.setColorIdentity(toColorIdentity(commanders, resolved));
        deckRepository.persist(deck);
        LOG.infov("event=deck.created deckId={0}", deck.getId());

        return toDto(deck);
    }

    @Transactional
    public DeckResponseDTO importDeck(DeckImportDTO dto, String ownerId) {
        return importDeck(dto, ownerId, null);
    }

    @Transactional
    public DeckResponseDTO importDeck(DeckImportDTO dto, AuthenticatedUserDTO user) {
        validateUser(user);
        return importDeck(dto, user.googleSubject(), user.name());
    }

    @Transactional
    public DeckResponseDTO importDeck(DeckImportDTO dto, String ownerId, String authorDisplayName) {
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
        deck.setAuthorDisplayName(toPublicAuthor(authorDisplayName));
        deck.setVisibility(normalizeVisibility(dto.visibility()));
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

    public List<PublicDeckSummaryDTO> listPublicDecks(Integer page, Integer size, String commander, String currentOwnerId) {
        return deckRepository.listPublic(page, size, commander).stream()
                .map(deck -> toPublicSummary(deck, currentOwnerId))
                .collect(Collectors.toList());
    }

    public List<PublicDeckSummaryDTO> listTopPublicDecks(String period, Integer size, String currentOwnerId) {
        OffsetDateTime since = sinceForPeriod(period);
        int safeSize = size == null ? 24 : Math.max(1, Math.min(size, 50));
        return deckLikeRepository.listTopPublicDecks(since, safeSize).stream()
                .map(deck -> toPublicSummary(deck, currentOwnerId))
                .collect(Collectors.toList());
    }

    public DeckConsultResponseDTO consultDeck(Long id, String ownerId) {
        Deck deck = deckRepository.findPublicByIdOrOwner(id, ownerId);
        if (deck == null) {
            return null;
        }
        return toConsultDto(deck);
    }

    public PublicDeckResponseDTO getPublicDeck(Long id, String currentOwnerId) {
        Deck deck = deckRepository.findPublicById(id);
        if (deck == null) {
            return null;
        }
        return toPublicDto(deck, currentOwnerId);
    }

    @Transactional
    public DeckResponseDTO copyPublicDeck(Long id, AuthenticatedUserDTO user) {
        validateUser(user);
        Deck source = deckRepository.findPublicById(id);
        if (source == null) {
            return null;
        }

        Deck copy = new Deck();
        copy.setName(copyName(source.getName()));
        copy.setCommander(source.getCommander());
        copy.setOwnerId(user.googleSubject());
        copy.setAuthorDisplayName(toPublicAuthor(user.name()));
        copy.setVisibility(DeckVisibility.PRIVATE);
        copy.setCommandersJson(source.getCommandersJson());
        copy.setColorIdentity(source.getColorIdentity());
        copy.setCards(source.getCards().stream()
                .map(card -> new DeckCard(card.getName(), card.getQuantity()))
                .collect(Collectors.toList()));
        deckRepository.persist(copy);
        LOG.infov("event=deck.public.copy.created sourceDeckId={0} copiedDeckId={1}", id, copy.getId());
        return toDto(copy);
    }

    @Transactional
    public PublicDeckResponseDTO likePublicDeck(Long id, String ownerId) {
        validateOwner(ownerId);
        Deck deck = deckRepository.findPublicById(id);
        if (deck == null) {
            return null;
        }
        if (deckLikeRepository.findByDeckAndOwner(id, ownerId) == null) {
            DeckLike like = new DeckLike();
            like.setDeck(deck);
            like.setOwnerId(ownerId);
            like.setCreatedAt(OffsetDateTime.now());
            deckLikeRepository.persist(like);
        }
        return toPublicDto(deck, ownerId);
    }

    @Transactional
    public boolean unlikePublicDeck(Long id, String ownerId) {
        validateOwner(ownerId);
        Deck deck = deckRepository.findPublicById(id);
        if (deck == null) {
            return false;
        }
        deckLikeRepository.deleteByDeckAndOwner(id, ownerId);
        return true;
    }

    @Transactional
    public DeckResponseDTO updateDeck(Long id, DeckRequestDTO request, String ownerId) {
        return updateDeck(id, request, ownerId, null);
    }

    @Transactional
    public DeckResponseDTO updateDeck(Long id, DeckRequestDTO request, AuthenticatedUserDTO user) {
        validateUser(user);
        return updateDeck(id, request, user.googleSubject(), user.name());
    }

    @Transactional
    public DeckResponseDTO updateDeck(Long id, DeckRequestDTO request, String ownerId, String authorDisplayName) {
        validateRequest(request);
        validateOwner(ownerId);
        Deck deck = deckRepository.findByIdAndOwner(id, ownerId);
        if (deck == null) {
            return null;
        }
        LOG.debugv("event=deck.update.request deckId={0}", id);
        List<CommanderDTO> commanders = normalizeCommanders(request.commander(), request.commanders());
        boolean commandersChanged = !sameCommanders(commandersFor(deck), commanders);
        boolean cardsChanged = !sameCards(deck.getCards(), request.cards());
        Map<String, CardResponseDTO> resolved = commandersChanged || cardsChanged
                ? validateCardsExist(commanders, request.cards())
                : Map.of();
        deck.setName(request.name().trim());
        deck.setCommander(primaryCommander(commanders));
        deck.setCommandersJson(toCommandersJson(commanders));
        if (commandersChanged || cardsChanged) {
            deck.setColorIdentity(toColorIdentity(commanders, resolved));
        }
        deck.setVisibility(normalizeVisibility(request.visibility()));
        String publicAuthor = toPublicAuthor(authorDisplayName);
        if (publicAuthor != null) {
            deck.setAuthorDisplayName(publicAuthor);
        }
        if (cardsChanged) {
            deck.setCards(toEntities(request.cards()));
        }
        deckRepository.persist(deck);
        LOG.infov("event=deck.updated deckId={0}", id);
        return toDto(deck);
    }

    @Transactional
    public boolean deleteDeck(Long id, String ownerId) {
        validateOwner(ownerId);
        LOG.infov("event=deck.delete.request deckId={0}", id);
        deckLikeRepository.deleteByDeck(id);
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
            throw new IllegalArgumentException("Deck must have at most 99 cards");
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
            throw new IllegalArgumentException("Deck must have at most 99 cards");
        }

        deckRepository.persist(deck);
        LOG.infov("event=recommendation.swap.applied deckId={0}", deckId);
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
            throw new IllegalArgumentException("Card added by the swap is no longer in the deck");
        }
        DeckCard removedCard = findMainDeckCard(deck, entry.remove());
        if (removedCard != null && !isBasicLand(entry.remove())) {
            throw new IllegalArgumentException("Card removed by the swap is already back in the deck");
        }

        removeOne(deck, addedCard);
        addOne(deck, entry.remove(), removedCard);
        replaceHistory(deck, markUndone(history, entry.id()));
        deckRepository.persist(deck);
        LOG.infov("event=recommendation.swap.undone deckId={0}", deckId);
        return toDto(deck);
    }

    public String exportDeck(Long id, String ownerId) {
        validateOwner(ownerId);
        LOG.infov("event=deck.export.request deckId={0}", id);
        Deck deck = deckRepository.findByIdAndOwner(id, ownerId);
        if (deck == null) {
            LOG.errorv("event=deck.export.not_found deckId={0}", id);
            return null;
        }
        List<DeckCard> cards = mainDeckCards(deck);
        LOG.debugv("event=deck.export.ready deckId={0} cardEntries={1}", id, cards == null ? 0 : cards.size());
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
                .mapToInt(DeckCardDTO::quantity)
                .sum();
        if (mainDeckTotal <= 0) {
            throw new IllegalArgumentException("Deck must contain at least one card");
        }
        if (mainDeckTotal > MAX_COMMANDER_MAIN_DECK_CARDS) {
            throw new IllegalArgumentException("Commander deck must have at most 99 cards");
        }
    }

    private List<DeckCard> toEntities(List<DeckCardDTO> cards) {
        return cards.stream()
                .map(c -> new DeckCard(c.name().trim(), c.quantity()))
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
    }

    private Map<String, CardResponseDTO> validateCardsExist(List<CommanderDTO> commanders, List<DeckCardDTO> cards) {
        List<String> names = new java.util.ArrayList<>();
        commanders.stream().map(CommanderDTO::name).forEach(names::add);
        cards.stream().map(DeckCardDTO::name).forEach(names::add);

        Map<String, CardResponseDTO> resolved = cardService.findByNames(names);
        for (String name : names) {
            if (!resolved.containsKey(cardService.normalizeLookupName(name))) {
                LOG.warn("event=deck.card_validation.failed reason=not_found");
                throw new IllegalArgumentException("Card not found: " + name.trim());
            }
        }
        return resolved;
    }

    private boolean sameCommanders(List<CommanderDTO> currentCommanders, List<CommanderDTO> requestedCommanders) {
        if (currentCommanders == null || requestedCommanders == null || currentCommanders.size() != requestedCommanders.size()) {
            return false;
        }
        for (int index = 0; index < currentCommanders.size(); index++) {
            CommanderDTO current = currentCommanders.get(index);
            CommanderDTO requested = requestedCommanders.get(index);
            if (!normalize(current.name()).equals(normalize(requested.name()))) {
                return false;
            }
            if (!normalizeRole(current.role()).equals(normalizeRole(requested.role()))) {
                return false;
            }
        }
        return true;
    }

    private boolean sameCards(List<DeckCard> currentCards, List<DeckCardDTO> requestedCards) {
        if (currentCards == null || requestedCards == null || currentCards.size() != requestedCards.size()) {
            return false;
        }
        Map<String, Integer> currentQuantities = currentCards.stream()
                .collect(Collectors.toMap(
                        card -> normalize(card.getName()),
                        DeckCard::getQuantity,
                        Integer::sum,
                        java.util.LinkedHashMap::new
                ));
        Map<String, Integer> requestedQuantities = requestedCards.stream()
                .collect(Collectors.toMap(
                        card -> normalize(card.name()),
                        DeckCardDTO::quantity,
                        Integer::sum,
                        java.util.LinkedHashMap::new
                ));
        return currentQuantities.equals(requestedQuantities);
    }

    private String normalizeRole(String role) {
        return role == null || role.isBlank() ? "commander" : role.trim().toLowerCase(Locale.ROOT).replace("_", "-");
    }

    private void validateCardExists(String name, String message) {
        Map<String, CardResponseDTO> resolved = cardService.findByNames(List.of(name));
        if (!resolved.containsKey(cardService.normalizeLookupName(name))) {
            LOG.warn("event=deck.card_validation.failed reason=not_found");
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

    private void validateUser(AuthenticatedUserDTO user) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        validateOwner(user.googleSubject());
    }

    private DeckVisibility normalizeVisibility(DeckVisibility visibility) {
        return visibility == null ? DeckVisibility.PRIVATE : visibility;
    }

    private String toPublicAuthor(String authorDisplayName) {
        if (authorDisplayName == null || authorDisplayName.isBlank()) {
            return null;
        }
        String trimmed = authorDisplayName.trim();
        return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
    }

    private String copyName(String sourceName) {
        String baseName = sourceName == null || sourceName.isBlank() ? "Deck" : sourceName.trim();
        String copiedName = "Copia de " + baseName;
        return copiedName.length() > MAX_DECK_NAME_LENGTH ? copiedName.substring(0, MAX_DECK_NAME_LENGTH) : copiedName;
    }

    private DeckResponseDTO toDto(Deck deck) {
        List<DeckCardDTO> cards = deck.getCards().stream()
                .map(c -> new DeckCardDTO(c.getName(), c.getQuantity()))
                .collect(Collectors.toList());
        return new DeckResponseDTO(deck.getId(), deck.getName(), deck.getCommander(), cards, deck.getColorIdentity(), commandersFor(deck), historyFor(deck), deck.getVisibility());
    }

    private DeckConsultResponseDTO toConsultDto(Deck deck) {
        List<DeckCardDTO> cards = deck.getCards().stream()
                .map(c -> new DeckCardDTO(c.getName(), c.getQuantity()))
                .collect(Collectors.toList());
        return new DeckConsultResponseDTO(
                deck.getId(),
                deck.getName(),
                deck.getCommander(),
                deck.getColorIdentity(),
                deck.getVisibility(),
                cards,
                commandersFor(deck),
                deck.getAuthorDisplayName()
        );
    }

    private PublicDeckResponseDTO toPublicDto(Deck deck, String currentOwnerId) {
        List<DeckCardDTO> cards = deck.getCards().stream()
                .map(c -> new DeckCardDTO(c.getName(), c.getQuantity()))
                .collect(Collectors.toList());
        return new PublicDeckResponseDTO(
                deck.getId(),
                deck.getName(),
                deck.getCommander(),
                commandersFor(deck),
                deck.getColorIdentity(),
                cards,
                totalCards(deck),
                deck.getVisibility(),
                deck.getAuthorDisplayName(),
                isOwnedBy(deck, currentOwnerId),
                deckLikeRepository.countByDeck(deck.getId()),
                deckLikeRepository.existsByDeckAndOwner(deck.getId(), currentOwnerId),
                deck.getSourceType(),
                deck.getExternalSource(),
                deck.getExternalSourceUrl(),
                deck.getExternalDeckUrl()
        );
    }

    private PublicDeckSummaryDTO toPublicSummary(Deck deck, String currentOwnerId) {
        return new PublicDeckSummaryDTO(
                deck.getId(),
                deck.getName(),
                deck.getCommander(),
                deck.getColorIdentity(),
                deck.getVisibility(),
                deck.getAuthorDisplayName(),
                totalCards(deck),
                isOwnedBy(deck, currentOwnerId),
                deckLikeRepository.countByDeck(deck.getId()),
                deckLikeRepository.existsByDeckAndOwner(deck.getId(), currentOwnerId),
                deck.getSourceType(),
                deck.getExternalSource(),
                deck.getExternalSourceUrl(),
                deck.getExternalDeckUrl()
        );
    }

    private OffsetDateTime sinceForPeriod(String period) {
        String normalized = period == null || period.isBlank()
                ? "WEEKLY"
                : period.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DAILY" -> OffsetDateTime.now().minus(1, ChronoUnit.DAYS);
            case "WEEKLY" -> OffsetDateTime.now().minus(7, ChronoUnit.DAYS);
            case "MONTHLY" -> OffsetDateTime.now().minus(30, ChronoUnit.DAYS);
            default -> throw new IllegalArgumentException("period must be DAILY, WEEKLY, or MONTHLY");
        };
    }

    private boolean isOwnedBy(Deck deck, String currentOwnerId) {
        return currentOwnerId != null
                && !currentOwnerId.isBlank()
                && currentOwnerId.equals(deck.getOwnerId());
    }

    private List<DeckCard> mainDeckCards(Deck deck) {
        return deck.getCards();
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
