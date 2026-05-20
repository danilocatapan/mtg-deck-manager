package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.CommanderDTO;
import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.ExternalDeckImportCardDTO;
import com.mtg.dto.ExternalDeckImportDeckDTO;
import com.mtg.dto.ExternalDeckImportRequestDTO;
import com.mtg.dto.ExternalDeckImportResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.model.DeckVisibility;
import com.mtg.repository.DeckRepository;
import com.mtg.service.meta.DecklistNormalizer;
import com.mtg.service.meta.MetaDeckCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class ExternalDeckImportService {
    private static final Logger LOG = Logger.getLogger(ExternalDeckImportService.class);
    private static final int MAX_DECKS_PER_IMPORT = 50;
    private static final int MAX_DECK_NAME_LENGTH = 120;
    private static final int MAX_CARD_NAME_LENGTH = 120;
    private static final int MAX_DECKLIST_LENGTH = 16_000;
    private static final Set<String> BASIC_LANDS = Set.of("plains", "island", "swamp", "mountain", "forest", "wastes");
    private static final Set<String> DECKLIST_FORMATS = Set.of("MTG_ARENA", "LIGAMAGIC", "GENERIC");

    @Inject
    DeckRepository deckRepository;

    @Inject
    CardService cardService;

    @Inject
    DecklistNormalizer normalizer;

    @ConfigProperty(name = "meta.external-import.owner-id", defaultValue = "external-import")
    String externalOwnerId;

    @Transactional
    public ExternalDeckImportResponseDTO importDecks(ExternalDeckImportRequestDTO request) {
        validateRequest(request);
        String source = request.source().trim();
        String format = normalizeFormat(request.format());
        String importFormat = normalizeImportFormat(request);
        String decklistFormat = normalizeDecklistFormat(request.decklistFormat());
        List<String> warnings = new ArrayList<>();
        int importedDecks = 0;
        int ignoredDecks = 0;
        int importedCards = 0;

        LOG.infov("event=meta.external_import.started source={0} decks={1}", source, request.decks().size());

        for (int index = 0; index < request.decks().size(); index++) {
            ExternalDeckImportDeckDTO imported = request.decks().get(index);
            try {
                ImportableDeck deck = toImportableDeck(index, imported, importFormat, decklistFormat, warnings);
                if (deck == null) {
                    ignoredDecks++;
                    continue;
                }
                Deck persisted = persistDeck(request, source, deck);
                importedDecks++;
                importedCards += persisted.getCards().stream().mapToInt(DeckCard::getQuantity).sum();
            } catch (IllegalArgumentException exception) {
                ignoredDecks++;
                warnings.add("Deck " + deckLabel(index, imported) + " ignored: " + exception.getMessage());
            } catch (RuntimeException exception) {
                ignoredDecks++;
                LOG.errorv(exception, "event=meta.external_import.deck_failed source={0}", source);
                warnings.add("Deck " + deckLabel(index, imported) + " ignored: import failed");
            }
        }

        LOG.infov(
                "event=meta.external_import.completed source={0} importedDecks={1} ignoredDecks={2} importedCards={3}",
                source,
                importedDecks,
                ignoredDecks,
                importedCards
        );
        warnings.forEach(warning -> LOG.warnv("event=meta.external_import.warning message=\"{0}\"", warning));
        return new ExternalDeckImportResponseDTO(source, format, importFormat, decklistFormat, importedDecks, ignoredDecks, importedCards, warnings);
    }

    private void validateRequest(ExternalDeckImportRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Import payload required");
        }
        if (request.source() == null || request.source().isBlank()) {
            throw new IllegalArgumentException("source is required");
        }
        if (request.source().trim().length() > 80) {
            throw new IllegalArgumentException("source is too long");
        }
        String format = normalizeFormat(request.format());
        if (!"COMMANDER".equals(format)) {
            throw new IllegalArgumentException("format must be COMMANDER");
        }
        if (request.decks() == null || request.decks().isEmpty()) {
            throw new IllegalArgumentException("decks must not be empty");
        }
        if (request.decks().size() > MAX_DECKS_PER_IMPORT) {
            throw new IllegalArgumentException("Import accepts at most " + MAX_DECKS_PER_IMPORT + " decks");
        }
        normalizeImportFormat(request);
        normalizeDecklistFormat(request.decklistFormat());
    }

    private ImportableDeck toImportableDeck(
            int index,
            ExternalDeckImportDeckDTO imported,
            String importFormat,
            String decklistFormat,
            List<String> warnings
    ) {
        if (imported == null) {
            throw new IllegalArgumentException("deck payload required");
        }

        List<DeckCardDTO> requestedCards = "TEXT".equals(importFormat)
                ? parseTextCards(imported, decklistFormat)
                : parseStructuredCards(imported.cards());

        String commander = firstPresent(imported.commander(), "TEXT".equals(importFormat) ? normalizer.findCommander(imported.decklist()) : null);
        validateCardName(commander, "commander");
        if (requestedCards.isEmpty()) {
            throw new IllegalArgumentException("cards must not be empty");
        }

        requestedCards = mergeCards(requestedCards).stream()
                .filter(card -> !sameName(card.name(), commander))
                .toList();
        if (requestedCards.isEmpty()) {
            throw new IllegalArgumentException("cards must include main deck cards");
        }

        validateDuplicates(requestedCards);
        int mainDeckTotal = requestedCards.stream().mapToInt(DeckCardDTO::quantity).sum();
        if (mainDeckTotal != 99) {
            warnings.add("Deck " + deckLabel(index, imported) + " has " + mainDeckTotal + " main-deck cards; expected 99 for Commander.");
        }

        List<String> namesToResolve = new ArrayList<>();
        namesToResolve.add(commander);
        requestedCards.stream().map(DeckCardDTO::name).forEach(namesToResolve::add);
        Map<String, CardResponseDTO> resolved = cardService.findByNames(namesToResolve);
        CardResponseDTO commanderCard = resolved.get(cardService.normalizeLookupName(commander));
        if (commanderCard == null) {
            warnings.add("Deck " + deckLabel(index, imported) + " ignored: commander not resolved: " + commander.trim());
            return null;
        }

        List<DeckCardDTO> resolvedCards = new ArrayList<>();
        for (DeckCardDTO card : requestedCards) {
            if (resolved.containsKey(cardService.normalizeLookupName(card.name()))) {
                resolvedCards.add(card);
            } else {
                warnings.add("Deck " + deckLabel(index, imported) + " skipped unresolved card: " + card.name().trim());
            }
        }
        if (resolvedCards.isEmpty()) {
            warnings.add("Deck " + deckLabel(index, imported) + " ignored: no resolved main-deck cards.");
            return null;
        }

        return new ImportableDeck(
                trimmedOrDefault(imported.name(), commander.trim()),
                commander.trim(),
                toColorIdentity(commanderCard),
                resolvedCards,
                imported.deckUrl(),
                imported.rank()
        );
    }

    private Deck persistDeck(ExternalDeckImportRequestDTO request, String source, ImportableDeck imported) {
        Deck deck = new Deck();
        deck.setName(truncate(imported.name(), MAX_DECK_NAME_LENGTH));
        deck.setCommander(imported.commander());
        deck.setOwnerId(externalOwnerId);
        deck.setAuthorDisplayName(null);
        deck.setVisibility(DeckVisibility.PUBLIC);
        deck.setColorIdentity(imported.colorIdentity());
        deck.setCommandersJson(toCommandersJson(imported.commander()));
        deck.setSourceType("external");
        deck.setExternalSource(source);
        deck.setExternalSourceUrl(blankToNull(request.sourceUrl()));
        deck.setExternalDeckUrl(blankToNull(imported.deckUrl()));
        deck.setExternalRank(imported.externalRank());
        deck.setImportedAt(OffsetDateTime.now());
        deck.setCards(imported.cards().stream()
                .map(card -> new DeckCard(card.name().trim(), card.quantity()))
                .collect(Collectors.toList()));
        deckRepository.persist(deck);
        return deck;
    }

    private List<DeckCardDTO> parseStructuredCards(List<ExternalDeckImportCardDTO> cards) {
        if (cards == null || cards.isEmpty()) {
            throw new IllegalArgumentException("cards must not be empty");
        }
        List<DeckCardDTO> parsed = new ArrayList<>();
        for (ExternalDeckImportCardDTO card : cards) {
            if (card == null) {
                continue;
            }
            String section = card.section() == null || card.section().isBlank()
                    ? "MAIN"
                    : card.section().trim().toUpperCase(Locale.ROOT);
            if (!Set.of("MAIN", "COMMANDER").contains(section)) {
                throw new IllegalArgumentException("card section must be MAIN or COMMANDER");
            }
            if ("COMMANDER".equals(section)) {
                continue;
            }
            validateCardName(card.name(), "card name");
            if (card.quantity() < 1 || card.quantity() > 99) {
                throw new IllegalArgumentException("card quantity must be between 1 and 99");
            }
            parsed.add(new DeckCardDTO(card.name().trim(), card.quantity()));
        }
        return parsed;
    }

    private List<DeckCardDTO> parseTextCards(ExternalDeckImportDeckDTO imported, String decklistFormat) {
        if (imported.decklist() == null || imported.decklist().isBlank()) {
            throw new IllegalArgumentException("decklist is required for TEXT import");
        }
        if (imported.decklist().length() > MAX_DECKLIST_LENGTH) {
            throw new IllegalArgumentException("decklist is too large");
        }
        if (!DECKLIST_FORMATS.contains(decklistFormat)) {
            throw new IllegalArgumentException("decklistFormat must be MTG_ARENA, LIGAMAGIC, or GENERIC");
        }
        List<MetaDeckCard> cards = normalizer.normalizePlainText(imported.decklist());
        return cards.stream()
                .map(card -> new DeckCardDTO(card.name(), card.quantity()))
                .toList();
    }

    private List<DeckCardDTO> mergeCards(List<DeckCardDTO> cards) {
        Map<String, DeckCardDTO> merged = new LinkedHashMap<>();
        for (DeckCardDTO card : cards) {
            validateCardName(card.name(), "card name");
            if (card.quantity() < 1 || card.quantity() > 99) {
                throw new IllegalArgumentException("card quantity must be between 1 and 99");
            }
            String key = normalize(card.name());
            DeckCardDTO existing = merged.get(key);
            if (existing == null) {
                merged.put(key, new DeckCardDTO(card.name().trim(), card.quantity()));
            } else {
                merged.put(key, new DeckCardDTO(existing.name(), existing.quantity() + card.quantity()));
            }
        }
        return new ArrayList<>(merged.values());
    }

    private void validateDuplicates(List<DeckCardDTO> cards) {
        for (DeckCardDTO card : cards) {
            if (card.quantity() > 1 && !isBasicLand(card.name())) {
                throw new IllegalArgumentException("duplicated non-basic card: " + card.name().trim());
            }
        }
    }

    private String normalizeFormat(String format) {
        return format == null || format.isBlank() ? "COMMANDER" : format.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeImportFormat(ExternalDeckImportRequestDTO request) {
        if (request.importFormat() != null && !request.importFormat().isBlank()) {
            String normalized = request.importFormat().trim().toUpperCase(Locale.ROOT);
            if (!Set.of("STRUCTURED", "TEXT").contains(normalized)) {
                throw new IllegalArgumentException("importFormat must be STRUCTURED or TEXT");
            }
            return normalized;
        }
        boolean hasTextDeck = request.decks().stream().anyMatch(deck -> deck != null && deck.decklist() != null && !deck.decklist().isBlank());
        return hasTextDeck ? "TEXT" : "STRUCTURED";
    }

    private String normalizeDecklistFormat(String decklistFormat) {
        if (decklistFormat == null || decklistFormat.isBlank()) {
            return "GENERIC";
        }
        String normalized = decklistFormat.trim().toUpperCase(Locale.ROOT);
        if (!DECKLIST_FORMATS.contains(normalized)) {
            throw new IllegalArgumentException("decklistFormat must be MTG_ARENA, LIGAMAGIC, or GENERIC");
        }
        return normalized;
    }

    private void validateCardName(String name, String field) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (name.trim().length() > MAX_CARD_NAME_LENGTH) {
            throw new IllegalArgumentException(field + " is too long");
        }
    }

    private String toColorIdentity(CardResponseDTO commanderCard) {
        if (commanderCard == null || commanderCard.colorIdentity() == null) {
            return "";
        }
        return commanderCard.colorIdentity().stream()
                .map(color -> color == null ? "" : color.trim().toUpperCase(Locale.ROOT))
                .filter(color -> Set.of("W", "U", "B", "R", "G", "C").contains(color))
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
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

    private String toCommandersJson(String commander) {
        return "[{\"name\":\"" + commander.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"role\":\"commander\"}]";
    }

    private String deckLabel(int index, ExternalDeckImportDeckDTO deck) {
        String name = deck == null ? null : deck.name();
        return name == null || name.isBlank() ? "#" + (index + 1) : "\"" + name.trim() + "\"";
    }

    private String firstPresent(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String trimmedOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean sameName(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private boolean isBasicLand(String name) {
        return BASIC_LANDS.contains(normalize(name));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ImportableDeck(
            String name,
            String commander,
            String colorIdentity,
            List<DeckCardDTO> cards,
            String deckUrl,
            Integer externalRank
    ) {
    }
}
