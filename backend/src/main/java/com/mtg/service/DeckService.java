package com.mtg.service;

import com.mtg.dto.DeckCardDTO;
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
import java.util.stream.Collectors;

@ApplicationScoped
public class DeckService {

    private static final Logger LOG = Logger.getLogger(DeckService.class);
    private static final int MAX_DECK_NAME_LENGTH = 120;
    private static final int MAX_CARD_NAME_LENGTH = 120;
    private static final int MAX_DECK_CARDS = 120;

    private final DeckRepository deckRepository;

    @Inject
    DeckImportService importService;

    @Inject
    public DeckService(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    @Transactional
    public DeckResponseDTO createDeck(DeckRequestDTO request, String ownerId) {
        validateRequest(request);
        validateOwner(ownerId);
        LOG.debug("Creating deck: " + request);

        List<DeckCard> cards = toEntities(request.cards());
        Deck deck = new Deck(request.name().trim(), request.commander().trim(), cards);
        deck.setOwnerId(ownerId);
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

        var cards = importService.parse(dto.content());
        int total = cards.stream().mapToInt(DeckCard::getQuantity).sum();
        if (total > 99) {
            throw new IllegalArgumentException("Imported deck has " + total + " cards; maximum is 99.");
        }

        Deck deck = new Deck();
        deck.setName(dto.name().trim());
        deck.setCommander(dto.commander().trim());
        deck.setOwnerId(ownerId);
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
        deck.setName(request.name().trim());
        deck.setCommander(request.commander().trim());
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

    public String exportDeck(Long id, String ownerId) {
        validateOwner(ownerId);
        LOG.info("Export requested: " + id);
        Deck deck = deckRepository.findByIdAndOwner(id, ownerId);
        if (deck == null) {
            LOG.error("Export failed: deck not found " + id);
            return null;
        }
        List<DeckCard> cards = deck.getCards();
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
        validateCommander(request.commander());
        if (request.cards() == null || request.cards().isEmpty()) {
            throw new IllegalArgumentException("Deck must contain at least one card");
        }
        if (request.cards().size() > MAX_DECK_CARDS) {
            throw new IllegalArgumentException("Deck accepts at most " + MAX_DECK_CARDS + " card entries");
        }
        request.cards().forEach(this::validateCard);
    }

    private List<DeckCard> toEntities(List<DeckCardDTO> cards) {
        return cards.stream().map(c -> new DeckCard(c.name().trim(), c.quantity())).collect(Collectors.toList());
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

    private void validateOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
    }

    private DeckResponseDTO toDto(Deck deck) {
        List<DeckCardDTO> cards = deck.getCards().stream().map(c -> new DeckCardDTO(c.getName(), c.getQuantity())).collect(Collectors.toList());
        return new DeckResponseDTO(deck.getId(), deck.getName(), deck.getCommander(), cards);
    }
}
