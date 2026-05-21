package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.CommanderDTO;
import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.MetaTopDeckCardRequestDTO;
import com.mtg.dto.MetaTopDeckCardResponseDTO;
import com.mtg.dto.MetaTopDeckDetailDTO;
import com.mtg.dto.MetaTopDeckImportDeckDTO;
import com.mtg.dto.MetaTopDeckImportRequestDTO;
import com.mtg.dto.MetaTopDeckImportResponseDTO;
import com.mtg.dto.MetaTopDeckSummaryDTO;
import com.mtg.dto.MetaTopDeckSyncRequestDTO;
import com.mtg.dto.MetaTopDeckSyncResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.model.DeckVisibility;
import com.mtg.model.MetaDeckArchetype;
import com.mtg.model.MetaDeckBracket;
import com.mtg.model.MetaDeckCardSection;
import com.mtg.model.MetaDeckFormat;
import com.mtg.model.MetaDeckSource;
import com.mtg.model.MetaImportStatus;
import com.mtg.model.MetaRankingPeriod;
import com.mtg.model.MetaTopDeck;
import com.mtg.model.MetaTopDeckCard;
import com.mtg.model.MetaTopDeckImportBatch;
import com.mtg.repository.DeckRepository;
import com.mtg.repository.MetaTopDeckImportBatchRepository;
import com.mtg.repository.MetaTopDeckRepository;
import com.mtg.service.meta.MetaTopDeckSignalBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDate;
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
public class MetaTopDeckService {
    private static final Logger LOG = Logger.getLogger(MetaTopDeckService.class);
    private static final int MAX_DECKS_PER_IMPORT = 100;
    private static final int MAX_NAME_LENGTH = 120;
    private static final Set<String> BASIC_LANDS = Set.of("plains", "island", "swamp", "mountain", "forest", "wastes");

    @Inject
    MetaTopDeckRepository topDeckRepository;

    @Inject
    MetaTopDeckImportBatchRepository batchRepository;

    @Inject
    DeckRepository deckRepository;

    @Inject
    CardService cardService;

    @Inject
    MetaTopDeckSignalBuilder signalBuilder;

    @ConfigProperty(name = "meta.top-decks.public-owner-id", defaultValue = "external-import")
    String publicOwnerId;

    @Transactional
    public MetaTopDeckImportResponseDTO importTopDecks(MetaTopDeckImportRequestDTO request) {
        ValidImportRequest valid = validateRequest(request);
        MetaTopDeckImportBatch batch = newBatch(valid.source(), valid.rankingPeriod(), valid.rankingDate());
        batchRepository.persist(batch);

        List<String> warnings = new ArrayList<>();
        List<String> affectedProfiles;
        int createdDecks = 0;
        int updatedDecks = 0;
        int ignoredDecks = 0;
        int importedCards = 0;
        Set<String> payloadRanks = new LinkedHashSet<>();

        LOG.infov(
                "event=meta.top_decks.import.started source={0} rankingPeriod={1} rankingDate={2} decks={3}",
                valid.source(),
                valid.rankingPeriod(),
                valid.rankingDate(),
                request.decks().size()
        );

        for (int index = 0; index < request.decks().size(); index++) {
            MetaTopDeckImportDeckDTO deckRequest = request.decks().get(index);
            try {
                ImportableTopDeck importable = toImportableDeck(valid, index, deckRequest, warnings);
                String rankKey = importable.commanderNormalized() + "|" + importable.rank();
                if (!payloadRanks.add(rankKey)) {
                    throw new IllegalArgumentException("rank must be unique for commander in the same payload");
                }
                MetaTopDeck existing = findExisting(valid, importable);
                boolean created = existing == null;
                MetaTopDeck persisted = created ? new MetaTopDeck() : existing;
                apply(valid, request.sourceUrl(), importable, persisted);
                syncPublicDeck(persisted);
                if (created) {
                    topDeckRepository.persist(persisted);
                    createdDecks++;
                } else {
                    updatedDecks++;
                }
                importedCards += persisted.getCards().stream().mapToInt(MetaTopDeckCard::getQuantity).sum();
            } catch (IllegalArgumentException exception) {
                ignoredDecks++;
                warnings.add("Deck " + deckLabel(index, deckRequest) + " ignored: " + exception.getMessage());
            } catch (RuntimeException exception) {
                ignoredDecks++;
                LOG.errorv(exception, "event=meta.top_decks.import.deck_failed source={0}", valid.source());
                warnings.add("Deck " + deckLabel(index, deckRequest) + " ignored: import failed");
            }
        }

        int importedDecks = createdDecks + updatedDecks;
        batch.setCreatedDecks(createdDecks);
        batch.setUpdatedDecks(updatedDecks);
        batch.setImportedDecks(importedDecks);
        batch.setIgnoredDecks(ignoredDecks);
        batch.setWarningsCount(warnings.size());
        batch.setStatus(statusFor(importedDecks, ignoredDecks));
        batch.setFinishedAt(OffsetDateTime.now());
        affectedProfiles = importedDecks > 0 && signalBuilder != null ? signalBuilder.refreshProfiles() : List.of();

        LOG.infov(
                "event=meta.top_decks.import.completed source={0} createdDecks={1} updatedDecks={2} ignoredDecks={3} affectedProfiles={4}",
                valid.source(),
                createdDecks,
                updatedDecks,
                ignoredDecks,
                affectedProfiles.size()
        );
        warnings.forEach(warning -> LOG.warnv("event=meta.top_decks.import.warning message=\"{0}\"", warning));

        return new MetaTopDeckImportResponseDTO(
                batch.getId(),
                valid.source().name(),
                valid.rankingPeriod().name(),
                valid.rankingDate(),
                valid.format().name(),
                batch.getStatus().name(),
                request.decks().size(),
                importedDecks,
                createdDecks,
                updatedDecks,
                ignoredDecks,
                importedCards,
                warnings.size(),
                importedDecks > 0 ? "REFRESHED" : "SKIPPED",
                affectedProfiles,
                warnings
        );
    }

    public List<MetaTopDeckSummaryDTO> list(
            String source,
            String rankingPeriod,
            LocalDate rankingDate,
            String format,
            String commander,
            String archetype,
            String bracket,
            String colorIdentity,
            Integer limit
    ) {
        return topDeckRepository.listFiltered(
                parseOptionalEnum(MetaDeckSource.class, source),
                parseOptionalEnum(MetaRankingPeriod.class, rankingPeriod),
                rankingDate,
                parseOptionalEnum(MetaDeckFormat.class, format),
                commander,
                parseOptionalEnum(MetaDeckArchetype.class, archetype),
                parseOptionalEnum(MetaDeckBracket.class, bracket),
                normalizeColorIdentity(colorIdentity),
                limit
        ).stream().map(this::toSummary).toList();
    }

    public MetaTopDeckDetailDTO get(Long id) {
        MetaTopDeck deck = topDeckRepository.findById(id);
        return deck == null ? null : toDetail(deck);
    }

    @Transactional
    public MetaTopDeckSyncResponseDTO sync(MetaTopDeckSyncRequestDTO request) {
        MetaDeckSource source = parseRequiredSource(request == null ? null : request.source());
        MetaRankingPeriod period = parseRequiredEnum(MetaRankingPeriod.class, request == null ? null : request.rankingPeriod(), "rankingPeriod is required");
        LocalDate rankingDate = request == null ? null : request.rankingDate();
        if (rankingDate == null) {
            throw new IllegalArgumentException("rankingDate is required");
        }
        MetaTopDeckImportBatch batch = newBatch(source, period, rankingDate);
        batch.setStatus(MetaImportStatus.SUCCESS);
        batch.setFinishedAt(OffsetDateTime.now());
        batchRepository.persist(batch);
        List<String> affectedProfiles = signalBuilder == null ? List.of() : signalBuilder.refreshProfiles();
        return new MetaTopDeckSyncResponseDTO(
                batch.getId(),
                MetaImportStatus.SUCCESS.name(),
                "Sincronizacao registrada com sucesso.",
                source.name(),
                period.name(),
                rankingDate,
                affectedProfiles.isEmpty() ? "SKIPPED" : "REFRESHED"
        );
    }

    private ValidImportRequest validateRequest(MetaTopDeckImportRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Import payload required");
        }
        MetaDeckSource source = parseRequiredSource(request.source());
        MetaRankingPeriod rankingPeriod = parseRequiredEnum(MetaRankingPeriod.class, request.rankingPeriod(), "rankingPeriod is required");
        if (request.rankingDate() == null) {
            throw new IllegalArgumentException("rankingDate is required");
        }
        MetaDeckFormat format = parseRequiredEnum(MetaDeckFormat.class, request.format(), "format is required");
        if (format != MetaDeckFormat.COMMANDER) {
            throw new IllegalArgumentException("format must be COMMANDER");
        }
        if (request.decks() == null || request.decks().isEmpty()) {
            throw new IllegalArgumentException("decks must not be empty");
        }
        if (request.decks().size() > MAX_DECKS_PER_IMPORT) {
            throw new IllegalArgumentException("Import accepts at most " + MAX_DECKS_PER_IMPORT + " decks");
        }
        return new ValidImportRequest(source, rankingPeriod, request.rankingDate(), format);
    }

    private ImportableTopDeck toImportableDeck(
            ValidImportRequest valid,
            int index,
            MetaTopDeckImportDeckDTO deck,
            List<String> warnings
    ) {
        if (deck == null) {
            throw new IllegalArgumentException("deck payload required");
        }
        if (deck.rank() == null || deck.rank() <= 0) {
            throw new IllegalArgumentException("rank must be greater than zero");
        }
        validateName(deck.commander(), "commander");
        List<MetaTopDeckCard> cards = parseCards(deck.cards());
        if (cards.isEmpty()) {
            throw new IllegalArgumentException("cards must not be empty");
        }

        List<MetaTopDeckCard> commanderCards = cards.stream()
                .filter(card -> card.getSection() == MetaDeckCardSection.COMMANDER)
                .toList();
        if (commanderCards.size() != 1 || commanderCards.getFirst().getQuantity() != 1) {
            throw new IllegalArgumentException("COMMANDER decks must include exactly one COMMANDER card with quantity 1");
        }
        if (!normalize(commanderCards.getFirst().getName()).equals(normalize(deck.commander()))) {
            throw new IllegalArgumentException("commander must match COMMANDER section card");
        }
        validateMainDuplicates(cards);
        int totalCards = cards.stream().mapToInt(MetaTopDeckCard::getQuantity).sum();
        if (totalCards < 100) {
            warnings.add("Deck " + deckLabel(index, deck) + " has " + totalCards + " total cards; expected 100 for Commander.");
        }
        if (totalCards > 100) {
            throw new IllegalArgumentException("Commander top deck must have at most 100 cards including commander");
        }

        List<String> namesToResolve = cards.stream().map(MetaTopDeckCard::getName).toList();
        Map<String, CardResponseDTO> resolved = cardService.findByNames(namesToResolve);
        CardResponseDTO commanderCard = resolved.get(cardService.normalizeLookupName(deck.commander()));
        if (commanderCard == null) {
            throw new IllegalArgumentException("commander not resolved: " + deck.commander().trim());
        }
        for (MetaTopDeckCard card : cards) {
            if (resolved.get(cardService.normalizeLookupName(card.getName())) == null) {
                warnings.add("Deck " + deckLabel(index, deck) + " contains unresolved card: " + card.getName());
            }
        }
        String colorIdentity = normalizeColorIdentity(deck.colorIdentity());
        if (colorIdentity == null) {
            colorIdentity = colorIdentityFrom(commanderCard);
        }
        return new ImportableTopDeck(
                deck.rank(),
                trimmedOrDefault(deck.name(), deck.commander().trim()),
                deck.commander().trim(),
                normalize(deck.commander()),
                blankToNull(deck.deckUrl()),
                parseArchetype(deck.archetype()),
                parseBracket(deck.bracket()),
                colorIdentity,
                deck.wins(),
                deck.losses(),
                deck.popularityScore(),
                cards
        );
    }

    private List<MetaTopDeckCard> parseCards(List<MetaTopDeckCardRequestDTO> requestedCards) {
        if (requestedCards == null || requestedCards.isEmpty()) {
            throw new IllegalArgumentException("cards must not be empty");
        }
        Map<String, MetaTopDeckCard> merged = new LinkedHashMap<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (MetaTopDeckCardRequestDTO requested : requestedCards) {
            if (requested == null) {
                continue;
            }
            validateName(requested.name(), "card name");
            if (requested.quantity() <= 0) {
                throw new IllegalArgumentException("card quantity must be greater than zero");
            }
            MetaDeckCardSection section = parseRequiredEnum(MetaDeckCardSection.class, requested.section(), "card section is required");
            String normalized = normalize(requested.name());
            String key = section.name() + "|" + normalized;
            MetaTopDeckCard existing = merged.get(key);
            if (existing == null) {
                MetaTopDeckCard card = new MetaTopDeckCard();
                card.setName(requested.name().trim());
                card.setNameNormalized(normalized);
                card.setQuantity(requested.quantity());
                card.setSection(section);
                card.setCreatedAt(now);
                merged.put(key, card);
            } else {
                existing.setQuantity(existing.getQuantity() + requested.quantity());
            }
        }
        return new ArrayList<>(merged.values());
    }

    private void validateMainDuplicates(List<MetaTopDeckCard> cards) {
        for (MetaTopDeckCard card : cards) {
            if (card.getSection() == MetaDeckCardSection.MAIN && card.getQuantity() > 1 && !isBasicLand(card.getName())) {
                throw new IllegalArgumentException("duplicated non-basic card: " + card.getName());
            }
        }
    }

    private MetaTopDeck findExisting(ValidImportRequest valid, ImportableTopDeck importable) {
        MetaTopDeck existing = topDeckRepository.findSameSnapshotByDeckUrl(
                valid.source(),
                valid.rankingPeriod(),
                valid.rankingDate(),
                importable.deckUrl()
        );
        if (existing != null) {
            return existing;
        }
        return topDeckRepository.findSameSnapshotByRank(
                valid.source(),
                valid.rankingPeriod(),
                valid.rankingDate(),
                valid.format(),
                importable.commanderNormalized(),
                importable.rank()
        );
    }

    private void apply(ValidImportRequest valid, String sourceUrl, ImportableTopDeck importable, MetaTopDeck deck) {
        OffsetDateTime now = OffsetDateTime.now();
        if (deck.getCreatedAt() == null) {
            deck.setCreatedAt(now);
        }
        deck.setUpdatedAt(now);
        deck.setSource(valid.source());
        deck.setSourceUrl(blankToNull(sourceUrl));
        deck.setDeckUrl(importable.deckUrl());
        deck.setName(truncate(importable.name()));
        deck.setFormat(valid.format());
        deck.setCommander(importable.commander());
        deck.setCommanderNormalized(importable.commanderNormalized());
        deck.setRank(importable.rank());
        deck.setRankingPeriod(valid.rankingPeriod());
        deck.setRankingDate(valid.rankingDate());
        deck.setArchetype(importable.archetype());
        deck.setBracket(importable.bracket());
        deck.setColorIdentity(importable.colorIdentity());
        deck.setWins(importable.wins());
        deck.setLosses(importable.losses());
        deck.setPopularityScore(importable.popularityScore());
        deck.setCards(importable.cards());
    }

    private void syncPublicDeck(MetaTopDeck topDeck) {
        Deck publicDeck = topDeck.getPublicDeck();
        if (publicDeck == null) {
            publicDeck = new Deck();
            topDeck.setPublicDeck(publicDeck);
        }
        publicDeck.setName(truncate(topDeck.getName()));
        publicDeck.setCommander(topDeck.getCommander());
        publicDeck.setOwnerId(publicOwnerId);
        publicDeck.setAuthorDisplayName(null);
        publicDeck.setVisibility(DeckVisibility.PUBLIC);
        publicDeck.setColorIdentity(topDeck.getColorIdentity());
        publicDeck.setCommandersJson(toCommandersJson(topDeck.getCommander()));
        publicDeck.setSourceType("external");
        publicDeck.setExternalSource(topDeck.getSource().name());
        publicDeck.setExternalSourceUrl(topDeck.getSourceUrl());
        publicDeck.setExternalDeckUrl(topDeck.getDeckUrl());
        publicDeck.setExternalRank(topDeck.getRank());
        publicDeck.setImportedAt(OffsetDateTime.now());
        List<DeckCard> mainCards = topDeck.getCards().stream()
                .filter(card -> card.getSection() == MetaDeckCardSection.MAIN)
                .filter(card -> !normalize(card.getName()).equals(topDeck.getCommanderNormalized()))
                .map(card -> new DeckCard(card.getName(), card.getQuantity()))
                .collect(Collectors.toList());
        publicDeck.setCards(mainCards);
        if (publicDeck.getId() == null) {
            deckRepository.persist(publicDeck);
        }
    }

    private MetaTopDeckImportBatch newBatch(MetaDeckSource source, MetaRankingPeriod period, LocalDate rankingDate) {
        MetaTopDeckImportBatch batch = new MetaTopDeckImportBatch();
        batch.setSource(source);
        batch.setRankingPeriod(period);
        batch.setRankingDate(rankingDate);
        batch.setStatus(MetaImportStatus.FAILED);
        batch.setCreatedAt(OffsetDateTime.now());
        return batch;
    }

    private MetaImportStatus statusFor(int importedDecks, int ignoredDecks) {
        if (importedDecks > 0 && ignoredDecks == 0) {
            return MetaImportStatus.SUCCESS;
        }
        if (importedDecks > 0) {
            return MetaImportStatus.PARTIAL_SUCCESS;
        }
        return MetaImportStatus.FAILED;
    }

    private MetaTopDeckSummaryDTO toSummary(MetaTopDeck deck) {
        return new MetaTopDeckSummaryDTO(
                deck.getId(),
                deck.getSource().name(),
                deck.getRankingPeriod().name(),
                deck.getRankingDate(),
                deck.getFormat().name(),
                deck.getRank(),
                deck.getName(),
                deck.getCommander(),
                deck.getDeckUrl(),
                deck.getArchetype().name(),
                deck.getBracket().name(),
                colorList(deck.getColorIdentity()),
                deck.getCards().stream().mapToInt(MetaTopDeckCard::getQuantity).sum(),
                deck.getPopularityScore()
        );
    }

    private MetaTopDeckDetailDTO toDetail(MetaTopDeck deck) {
        return new MetaTopDeckDetailDTO(
                deck.getId(),
                deck.getSource().name(),
                deck.getSourceUrl(),
                deck.getDeckUrl(),
                deck.getRankingPeriod().name(),
                deck.getRankingDate(),
                deck.getFormat().name(),
                deck.getRank(),
                deck.getName(),
                deck.getCommander(),
                deck.getArchetype().name(),
                deck.getBracket().name(),
                colorList(deck.getColorIdentity()),
                deck.getWins(),
                deck.getLosses(),
                deck.getPopularityScore(),
                deck.getCards().stream()
                        .sorted(Comparator.comparing(MetaTopDeckCard::getSection).thenComparing(MetaTopDeckCard::getName))
                        .map(card -> new MetaTopDeckCardResponseDTO(card.getName(), card.getQuantity(), card.getSection().name(), card.getScryfallId()))
                        .toList()
        );
    }

    private MetaDeckSource parseRequiredSource(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("source is required");
        }
        String normalized = enumName(value);
        if ("TOPDECK".equals(normalized) || "TOPDECKGG".equals(normalized)) {
            return MetaDeckSource.TOPDECK_GG;
        }
        return parseRequiredEnum(MetaDeckSource.class, normalized, "source is required");
    }

    private MetaDeckArchetype parseArchetype(String value) {
        MetaDeckArchetype parsed = parseOptionalEnum(MetaDeckArchetype.class, value);
        return parsed == null ? MetaDeckArchetype.UNKNOWN : parsed;
    }

    private MetaDeckBracket parseBracket(String value) {
        MetaDeckBracket parsed = parseOptionalEnum(MetaDeckBracket.class, value);
        return parsed == null ? MetaDeckBracket.UNKNOWN : parsed;
    }

    private <T extends Enum<T>> T parseRequiredEnum(Class<T> type, String value, String requiredMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(requiredMessage);
        }
        T parsed = parseOptionalEnum(type, value);
        if (parsed == null) {
            throw new IllegalArgumentException(type.getSimpleName().replace("Meta", "") + " is invalid");
        }
        return parsed;
    }

    private <T extends Enum<T>> T parseOptionalEnum(Class<T> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, enumName(value));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String enumName(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace('.', '_').replace(" ", "_");
    }

    private String colorIdentityFrom(CardResponseDTO commanderCard) {
        if (commanderCard == null || commanderCard.colorIdentity() == null) {
            return "";
        }
        return normalizeColorIdentity(commanderCard.colorIdentity());
    }

    private String normalizeColorIdentity(List<String> colors) {
        if (colors == null || colors.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String color : colors) {
            if (color == null || color.isBlank()) {
                continue;
            }
            String symbol = color.trim().toUpperCase(Locale.ROOT);
            if (!Set.of("W", "U", "B", "R", "G").contains(symbol)) {
                throw new IllegalArgumentException("colorIdentity must contain only W, U, B, R, or G");
            }
            normalized.add(symbol);
        }
        return sortColors(normalized);
    }

    private String normalizeColorIdentity(String colors) {
        if (colors == null || colors.isBlank()) {
            return null;
        }
        List<String> symbols = colors.trim().toUpperCase(Locale.ROOT).chars()
                .mapToObj(value -> String.valueOf((char) value))
                .toList();
        return normalizeColorIdentity(symbols);
    }

    private String sortColors(Set<String> colors) {
        return colors.stream()
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
            default -> 5;
        };
    }

    private List<String> colorList(String colors) {
        if (colors == null || colors.isBlank()) {
            return List.of();
        }
        return colors.chars().mapToObj(value -> String.valueOf((char) value)).toList();
    }

    private String toCommandersJson(String commander) {
        try {
            return com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                    .writeValueAsString(List.of(new CommanderDTO(commander, "commander")));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to serialize commander");
        }
    }

    private void validateName(String name, String field) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(field + " is too long");
        }
    }

    private String deckLabel(int index, MetaTopDeckImportDeckDTO deck) {
        String name = deck == null ? null : deck.name();
        return name == null || name.isBlank() ? "#" + (index + 1) : "\"" + name.trim() + "\"";
    }

    private String trimmedOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_NAME_LENGTH ? value : value.substring(0, MAX_NAME_LENGTH);
    }

    private boolean isBasicLand(String name) {
        return BASIC_LANDS.contains(normalize(name));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ValidImportRequest(
            MetaDeckSource source,
            MetaRankingPeriod rankingPeriod,
            LocalDate rankingDate,
            MetaDeckFormat format
    ) {
    }

    private record ImportableTopDeck(
            int rank,
            String name,
            String commander,
            String commanderNormalized,
            String deckUrl,
            MetaDeckArchetype archetype,
            MetaDeckBracket bracket,
            String colorIdentity,
            Integer wins,
            Integer losses,
            Double popularityScore,
            List<MetaTopDeckCard> cards
    ) {
    }
}
