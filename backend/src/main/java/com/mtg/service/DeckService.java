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

import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

import jakarta.inject.Inject;
import com.mtg.dto.DeckImportDTO;


@ApplicationScoped
public class DeckService {

    private static final Logger LOG = Logger.getLogger(DeckService.class);

    private final DeckRepository deckRepository;

    @Inject
    DeckImportService importService;

    @Inject
    public DeckService(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    @Transactional
    public DeckResponseDTO createDeck(DeckRequestDTO request) {
        validateRequest(request);
        LOG.debug("Creating deck: " + request);

        List<DeckCard> cards = toEntities(request.cards());
        Deck deck = new Deck(request.name(), request.commander(), cards);
        deckRepository.persist(deck);
        LOG.info("Deck created: " + deck.getId());

        return toDto(deck);
    }

    @Transactional
    public DeckResponseDTO importDeck(DeckImportDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Import payload required");
        if (dto.name() == null || dto.name().isBlank()) throw new IllegalArgumentException("Deck name required");
        if (dto.commander() == null || dto.commander().isBlank()) throw new IllegalArgumentException("Commander is required");

        var cards = importService.parse(dto.content());
        int total = cards.stream().mapToInt(c -> c.getQuantity()).sum();
        if (total > 99) throw new IllegalArgumentException("Imported deck exceeds 99 cards");

        Deck deck = new Deck();
        deck.setName(dto.name());
        deck.setCommander(dto.commander());
        deck.setCards(cards);
        deckRepository.persist(deck);
        return toDto(deck);
    }

    public List<DeckResponseDTO> listDecks() {
        return deckRepository.listAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public DeckResponseDTO getDeckById(Long id) {
        Deck deck = deckRepository.findById(id);
        if (deck == null) {
            return null;
        }
        return toDto(deck);
    }

    @Transactional
    public DeckResponseDTO updateDeck(Long id, DeckRequestDTO request) {
        validateRequest(request);
        Deck deck = deckRepository.findById(id);
        if (deck == null) {
            return null;
        }
        LOG.debug("Updating deck: " + id);
        deck.setName(request.name());
        deck.setCommander(request.commander());
        deck.setCards(toEntities(request.cards()));
        deckRepository.persist(deck);
        LOG.info("Deck updated: " + id);
        return toDto(deck);
    }

    @Transactional
    public boolean deleteDeck(Long id) {
        LOG.info("Deleting deck: " + id);
        return deckRepository.deleteById(id);
    }

    public String exportDeck(Long id) {
        LOG.info("Export requested: " + id);
        Deck deck = deckRepository.findById(id);
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
        if (request.cards() == null || request.cards().isEmpty()) {
            throw new IllegalArgumentException("Deck must contain at least one card");
        }
    }

    private List<DeckCard> toEntities(List<DeckCardDTO> cards) {
        return cards.stream().map(c -> new DeckCard(c.name(), c.quantity())).collect(Collectors.toList());
    }

    private DeckResponseDTO toDto(Deck deck) {
        List<DeckCardDTO> cards = deck.getCards().stream().map(c -> new DeckCardDTO(c.getName(), c.getQuantity())).collect(Collectors.toList());
        return new DeckResponseDTO(deck.getId(), deck.getName(), deck.getCommander(), cards);
    }
}
