package com.mtg.service;

import com.mtg.client.ScryfallClient;
import com.mtg.dto.CardLookupRequestDTO;
import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.ScryfallCardDTO;
import com.mtg.dto.ScryfallCollectionRequestDTO;
import com.mtg.dto.ScryfallCollectionResponseDTO;
import com.mtg.dto.ScryfallResponseDTO;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class CardService {

    private static final Logger LOG = Logger.getLogger(CardService.class);
    private static final int SCRYFALL_COLLECTION_LIMIT = 75;
    private static final int MAX_CARD_NAME_LENGTH = 120;
    private static final int MAX_QUERY_LENGTH = 180;
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 150L;

    @ConfigProperty(name = "scryfall.api.url")
    String scryfallApiUrl;

    private final ScryfallClient scryfallClient;
    private final Map<String, CardResponseDTO> cardLookupCache = new ConcurrentHashMap<>();
    private final Object requestThrottleLock = new Object();
    private long lastScryfallRequestAt;

    @Inject
    public CardService(@RestClient ScryfallClient scryfallClient) {
        this.scryfallClient = scryfallClient;
    }

    @CacheResult(cacheName = "cards-by-name")
    public List<CardResponseDTO> searchByName(String name) {
        String normalizedName = validateName(name);
        String query = buildNameQuery(normalizedName);

        LOG.info("event=cards.search.request");
        LOG.infov("event=scryfall.url base={0}", scryfallApiUrl);
        LOG.info("event=scryfall.query.prepared");
        LOG.debug("event=scryfall.search.start");

        try {
            throttleScryfallRequest();
            ScryfallResponseDTO response = scryfallClient.searchByName(query);
            List<CardResponseDTO> cards = mapResponse(response);
            cards.stream().findFirst().ifPresent(card -> cardLookupCache.put(normalizeLookupName(normalizedName), card));
            LOG.debugv("event=scryfall.search.success resultCount={0}", cards.size());
            return cards;
        } catch (NotFoundException exception) {
            LOG.debug("event=scryfall.search.empty");
            return List.of();
        } catch (WebApplicationException | ProcessingException exception) {
            if (isRateLimited(exception)) {
                LOG.warn("event=scryfall.rate_limited operation=search");
                throw new RateLimitedExternalServiceException("Scryfall rate limit exceeded", exception);
            }
            LOG.error("event=scryfall.search.failure");
            throw new ExternalServiceException("Failed to fetch cards from Scryfall", exception);
        }
    }

    public Map<String, CardResponseDTO> findByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Map.of();
        }

        List<String> uniqueNames = names.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();

        if (uniqueNames.isEmpty()) {
            return Map.of();
        }

        LOG.infov("event=cards.collection.request count={0}", uniqueNames.size());

        Map<String, CardResponseDTO> cardsByName = new LinkedHashMap<>();
        List<String> uncachedNames = new ArrayList<>();
        for (String name : uniqueNames) {
            CardResponseDTO cachedCard = cardLookupCache.get(normalizeLookupName(name));
            if (cachedCard == null) {
                uncachedNames.add(name);
            } else {
                cardsByName.put(normalizeLookupName(name), cachedCard);
            }
        }

        if (uncachedNames.isEmpty()) {
            LOG.debugv("event=cards.collection.cache.hit requested={0}", uniqueNames.size());
            return Map.copyOf(cardsByName);
        }

        List<String> misses = new ArrayList<>();

        for (int start = 0; start < uncachedNames.size(); start += SCRYFALL_COLLECTION_LIMIT) {
            List<String> chunk = uncachedNames.subList(start, Math.min(start + SCRYFALL_COLLECTION_LIMIT, uncachedNames.size()));
            try {
                throttleScryfallRequest();
                ScryfallCollectionResponseDTO response = scryfallClient.collection(toCollectionRequest(chunk));
                Map<String, CardResponseDTO> responseCards = mapCollectionResponse(response);
                for (String name : chunk) {
                    CardResponseDTO card = responseCards.get(normalizeLookupName(name));
                    if (card == null) {
                        card = responseCards.get(CardLookupRequestDTO.nameKey(name));
                    }
                    if (card != null) {
                        cardsByName.putIfAbsent(normalizeLookupName(name), card);
                    }
                }
                if (response != null && response.notFound() != null) {
                    response.notFound().stream()
                            .map(ScryfallCollectionRequestDTO.CardIdentifier::name)
                            .filter(Objects::nonNull)
                            .forEach(misses::add);
                }
            } catch (WebApplicationException | ProcessingException exception) {
                if (isRateLimited(exception)) {
                    LOG.warnv("event=scryfall.rate_limited operation=collection count={0}", chunk.size());
                    continue;
                }
                LOG.warnv("event=cards.collection.failure count={0}", chunk.size());
                misses.addAll(chunk);
            }
        }

        fetchMissesIndividually(misses, cardsByName);
        cardsByName.forEach(cardLookupCache::put);
        LOG.debugv("event=cards.collection.success requested={0} resolved={1}", uniqueNames.size(), cardsByName.size());
        return Map.copyOf(cardsByName);
    }

    public Map<String, CardResponseDTO> findByCardRequests(List<CardLookupRequestDTO> requests) {
        return findByCardRequests(requests, false);
    }

    public Map<String, CardResponseDTO> findByCardRequests(List<CardLookupRequestDTO> requests, boolean fallbackMissesIndividually) {
        if (requests == null || requests.isEmpty()) {
            return Map.of();
        }

        List<CardLookupRequestDTO> uniqueRequests = requests.stream()
                .filter(Objects::nonNull)
                .filter(request -> (request.scryfallId() != null && !request.scryfallId().isBlank())
                        || request.hasPrinting()
                        || (request.name() != null && !request.name().isBlank()))
                .collect(java.util.stream.Collectors.toMap(
                        CardLookupRequestDTO::lookupKey,
                        request -> request,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        if (uniqueRequests.isEmpty()) {
            return Map.of();
        }

        LOG.infov("event=cards.collection.request count={0}", uniqueRequests.size());
        Map<String, CardResponseDTO> cardsByRequest = new LinkedHashMap<>();
        List<CardLookupRequestDTO> uncachedRequests = new ArrayList<>();
        for (CardLookupRequestDTO request : uniqueRequests) {
            CardResponseDTO cachedCard = cardLookupCache.get(request.lookupKey());
            if (cachedCard == null && request.name() != null && !request.name().isBlank()) {
                cachedCard = cardLookupCache.get(CardLookupRequestDTO.nameKey(request.name()));
            }
            if (cachedCard == null) {
                uncachedRequests.add(request);
            } else {
                cardsByRequest.put(request.lookupKey(), cachedCard);
            }
        }

        List<String> misses = new ArrayList<>();
        for (int start = 0; start < uncachedRequests.size(); start += SCRYFALL_COLLECTION_LIMIT) {
            List<CardLookupRequestDTO> chunk = uncachedRequests.subList(start, Math.min(start + SCRYFALL_COLLECTION_LIMIT, uncachedRequests.size()));
            try {
                throttleScryfallRequest();
                ScryfallCollectionResponseDTO response = scryfallClient.collection(toCollectionRequestFromLookups(chunk));
                Map<String, CardResponseDTO> responseCards = mapCollectionResponse(response);
                for (CardLookupRequestDTO request : chunk) {
                    CardResponseDTO card = responseCards.get(request.lookupKey());
                    if (card == null && request.name() != null && !request.name().isBlank()) {
                        card = responseCards.get(CardLookupRequestDTO.nameKey(request.name()));
                    }
                    if (card == null) {
                        misses.add(request.name());
                    } else {
                        cardsByRequest.put(request.lookupKey(), card);
                    }
                }
            } catch (WebApplicationException | ProcessingException exception) {
                if (isRateLimited(exception)) {
                    LOG.warnv("event=scryfall.rate_limited operation=collection count={0}", chunk.size());
                    continue;
                }
                LOG.warnv("event=cards.collection.failure count={0}", chunk.size());
                chunk.stream().map(CardLookupRequestDTO::name).filter(Objects::nonNull).forEach(misses::add);
            }
        }

        if (fallbackMissesIndividually) {
            fetchMissesIndividually(misses, cardsByRequest);
        }
        cardsByRequest.forEach(cardLookupCache::put);
        return Map.copyOf(cardsByRequest);
    }

    @CacheResult(cacheName = "cards-by-query")
    public List<CardResponseDTO> searchByQuery(String query) {
        if (query == null || query.isBlank()) return List.of();
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Search query is too long");
        }

        LOG.info("event=cards.search.request");
        LOG.infov("event=scryfall.url base={0}", scryfallApiUrl);
        LOG.info("event=scryfall.query.prepared");
        LOG.debug("event=scryfall.search.start");

        try {
            throttleScryfallRequest();
            ScryfallResponseDTO response = scryfallClient.searchByName(query);
            List<CardResponseDTO> cards = mapResponse(response);
            LOG.debugv("event=scryfall.search.success resultCount={0}", cards.size());
            return cards;
        } catch (NotFoundException exception) {
            LOG.debug("event=scryfall.search.empty");
            return List.of();
        } catch (WebApplicationException | ProcessingException exception) {
            if (isRateLimited(exception)) {
                LOG.warn("event=scryfall.rate_limited operation=query");
                throw new RateLimitedExternalServiceException("Scryfall rate limit exceeded", exception);
            }
            LOG.error("event=scryfall.search.failure");
            throw new ExternalServiceException("Failed to fetch cards from Scryfall", exception);
        }
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The card name must be provided");
        }

        String trimmed = name.trim();
        if (trimmed.length() > MAX_CARD_NAME_LENGTH) {
            throw new IllegalArgumentException("The card name is too long");
        }

        return trimmed;
    }

    private String buildNameQuery(String name) {
        return "\"" + name.replace("\"", "\\\"") + "\"";
    }

    private ScryfallCollectionRequestDTO toCollectionRequest(List<String> names) {
        List<ScryfallCollectionRequestDTO.CardIdentifier> identifiers = names.stream()
                .map(ScryfallCollectionRequestDTO.CardIdentifier::new)
                .toList();

        return new ScryfallCollectionRequestDTO(identifiers);
    }

    private ScryfallCollectionRequestDTO toCollectionRequestFromLookups(List<CardLookupRequestDTO> requests) {
        List<ScryfallCollectionRequestDTO.CardIdentifier> identifiers = requests.stream()
                .map(request -> {
                    if (request.scryfallId() != null && !request.scryfallId().isBlank()) {
                        return new ScryfallCollectionRequestDTO.CardIdentifier(null, null, null, request.scryfallId().trim());
                    }
                    if (request.hasPrinting()) {
                        return ScryfallCollectionRequestDTO.CardIdentifier.printing(request.setCode().trim().toLowerCase(Locale.ROOT), request.collectorNumber().trim());
                    }
                    return new ScryfallCollectionRequestDTO.CardIdentifier(request.name().trim());
                })
                .toList();

        return new ScryfallCollectionRequestDTO(identifiers);
    }

    private Map<String, CardResponseDTO> mapCollectionResponse(ScryfallCollectionResponseDTO response) {
        if (response == null || response.data() == null) {
            return Map.of();
        }

        Map<String, CardResponseDTO> cardsByName = new LinkedHashMap<>();
        response.data().forEach(scryfallCard -> {
                    CardResponseDTO card = toCardResponse(scryfallCard);
                    cardsByName.putIfAbsent(normalizeLookupName(card.name()), card);
                    cardsByName.putIfAbsent(CardLookupRequestDTO.nameKey(card.name()), card);
                    lookupNames(scryfallCard).forEach(name -> {
                        cardsByName.putIfAbsent(normalizeLookupName(name), card);
                        cardsByName.putIfAbsent(CardLookupRequestDTO.nameKey(name), card);
                    });
                    if (card.setCode() != null && card.collectorNumber() != null) {
                        cardsByName.putIfAbsent(CardLookupRequestDTO.printingKey(card.setCode(), card.collectorNumber()), card);
                    }
                    if (card.scryfallId() != null) {
                        cardsByName.putIfAbsent("id:" + card.scryfallId().trim().toLowerCase(Locale.ROOT), card);
                    }
                });

        return cardsByName;
    }

    private void fetchMissesIndividually(List<String> misses, Map<String, CardResponseDTO> cardsByName) {
        for (String name : misses) {
            String lookupName = normalizeLookupName(name);
            if (cardsByName.containsKey(lookupName)) {
                continue;
            }

            try {
                List<CardResponseDTO> results = searchByName(name);
                if (!results.isEmpty()) {
                    cardsByName.put(lookupName, results.get(0));
                }
            } catch (RateLimitedExternalServiceException exception) {
                LOG.warn("event=cards.collection.miss.fallback.rate_limited");
                return;
            } catch (RuntimeException exception) {
                LOG.warn("event=cards.collection.miss.fallback.failed");
            }
        }
    }

    private List<CardResponseDTO> mapResponse(ScryfallResponseDTO response) {
        if (response == null || response.data() == null) {
            return List.of();
        }

        Map<String, CardResponseDTO> cardsByName = new LinkedHashMap<>();
        response.data().stream()
                .map(this::toCardResponse)
                .forEach(card -> cardsByName.putIfAbsent(card.name().toLowerCase(Locale.ROOT), card));

        return List.copyOf(cardsByName.values());
    }

    private CardResponseDTO toCardResponse(ScryfallCardDTO card) {
        ScryfallCardDTO.CardFaceDTO frontFace = frontFace(card);
        return new CardResponseDTO(
                firstPresent(frontFace == null ? null : frontFace.name(), card.name()),
                firstPresent(frontFace == null ? null : frontFace.manaCost(), card.manaCost()),
                firstPresent(frontFace == null ? null : frontFace.typeLine(), card.typeLine()),
                firstPresent(frontFace == null ? null : frontFace.oracleText(), card.oracleText()),
                card.cmc(),
                card.colorIdentity(),
                java.util.List.of(),
                imageUrl(card),
                estimatedPrice(card),
                card.id(),
                normalizeSetCode(card.set()),
                card.setName(),
                card.collectorNumber(),
                card.finishes() == null ? java.util.List.of() : card.finishes()
        );
    }

    private List<String> lookupNames(ScryfallCardDTO card) {
        if (card == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        if (card.name() != null && !card.name().isBlank()) {
            names.add(card.name());
        }
        if (card.cardFaces() != null) {
            card.cardFaces().stream()
                    .map(ScryfallCardDTO.CardFaceDTO::name)
                    .filter(name -> name != null && !name.isBlank())
                    .forEach(names::add);
        }
        return names;
    }

    private ScryfallCardDTO.CardFaceDTO frontFace(ScryfallCardDTO card) {
        if (card == null || card.cardFaces() == null || card.cardFaces().isEmpty()) {
            return null;
        }
        return card.cardFaces().get(0);
    }

    private String normalizeSetCode(String setCode) {
        return setCode == null || setCode.isBlank() ? null : setCode.trim().toUpperCase(Locale.ROOT);
    }

    private Double estimatedPrice(ScryfallCardDTO card) {
        if (card == null || card.prices() == null) {
            return null;
        }
        return parsePrice(card.prices().usd());
    }

    private Double parsePrice(String price) {
        if (price == null || price.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(price);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String imageUrl(ScryfallCardDTO card) {
        if (card == null) {
            return null;
        }
        if (card.imageUris() != null) {
            return preferredImage(card.imageUris());
        }
        if (card.cardFaces() != null && !card.cardFaces().isEmpty()
                && card.cardFaces().get(0) != null
                && card.cardFaces().get(0).imageUris() != null) {
            return preferredImage(card.cardFaces().get(0).imageUris());
        }
        return null;
    }

    private String preferredImage(ScryfallCardDTO.ImageUris imageUris) {
        if (imageUris.normal() != null && !imageUris.normal().isBlank()) return imageUris.normal();
        if (imageUris.large() != null && !imageUris.large().isBlank()) return imageUris.large();
        if (imageUris.small() != null && !imageUris.small().isBlank()) return imageUris.small();
        return imageUris.png();
    }

    private String firstPresent(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    public String normalizeLookupName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private void throttleScryfallRequest() {
        synchronized (requestThrottleLock) {
            long now = System.currentTimeMillis();
            long waitMillis = MIN_REQUEST_INTERVAL_MILLIS - (now - lastScryfallRequestAt);
            if (waitMillis > 0L) {
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
            lastScryfallRequestAt = System.currentTimeMillis();
        }
    }

    private boolean isRateLimited(Throwable exception) {
        if (exception instanceof WebApplicationException webException
                && webException.getResponse() != null
                && webException.getResponse().getStatus() == 429) {
            return true;
        }

        Throwable cause = exception.getCause();
        return cause != null && cause != exception && isRateLimited(cause);
    }
}
