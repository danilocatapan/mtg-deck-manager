package com.mtg.service;

import com.mtg.dto.UserCollectionCardDTO;
import com.mtg.dto.UserCollectionImportDTO;
import com.mtg.dto.UserCollectionImportResponseDTO;
import com.mtg.model.DeckCard;
import com.mtg.model.UserCardCollectionItem;
import com.mtg.repository.UserCardCollectionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserCollectionService {
    @Inject
    UserCardCollectionRepository repository;

    @Inject
    DeckImportService importService;

    @Transactional
    public UserCollectionImportResponseDTO importCollection(String ownerId, UserCollectionImportDTO request) {
        validateOwner(ownerId);
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Collection content is required");
        }

        List<DeckCard> parsed = importService.parse(request.getContent(), "GENERIC");
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("Collection content did not contain any card lines");
        }

        if (request.isReplaceExisting()) {
            repository.deleteByOwner(ownerId);
        }

        Map<String, CollectionLine> merged = new LinkedHashMap<>();
        for (DeckCard card : parsed) {
            String normalized = normalize(card.getName());
            if (normalized.isBlank()) {
                continue;
            }
            CollectionLine current = merged.get(normalized);
            int quantity = Math.max(1, card.getQuantity());
            if (current == null) {
                merged.put(normalized, new CollectionLine(card.getName().trim(), quantity));
            } else {
                merged.put(normalized, new CollectionLine(current.name(), current.quantity() + quantity));
            }
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (Map.Entry<String, CollectionLine> entry : merged.entrySet()) {
            UserCardCollectionItem item = repository.findByOwnerAndCard(ownerId, entry.getKey());
            if (item == null) {
                item = new UserCardCollectionItem();
                item.setOwnerId(ownerId);
                item.setCardNameNormalized(entry.getKey());
            }
            item.setCardName(entry.getValue().name());
            item.setQuantity(entry.getValue().quantity());
            item.setUpdatedAt(now);
            repository.persist(item);
        }

        int importedCards = merged.values().stream().mapToInt(CollectionLine::quantity).sum();
        List<String> warnings = new ArrayList<>();
        if (parsed.size() != merged.size()) {
            warnings.add("Cartas repetidas foram somadas na colecao importada.");
        }
        return new UserCollectionImportResponseDTO(importedCards, merged.size(), request.isReplaceExisting(), warnings);
    }

    public List<UserCollectionCardDTO> listCollection(String ownerId) {
        validateOwner(ownerId);
        return repository.listByOwner(ownerId).stream()
                .map(item -> new UserCollectionCardDTO(item.getCardName(), item.getQuantity() == null ? 0 : item.getQuantity()))
                .toList();
    }

    public Set<String> ownedCardNames(String ownerId) {
        validateOwner(ownerId);
        return repository.listByOwner(ownerId).stream()
                .filter(item -> item.getQuantity() != null && item.getQuantity() > 0)
                .map(UserCardCollectionItem::getCardNameNormalized)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasCollection(String ownerId) {
        return repository.countByOwner(ownerId) > 0;
    }

    @Transactional
    public void deleteCollection(String ownerId) {
        validateOwner(ownerId);
        repository.deleteByOwner(ownerId);
    }

    private void validateOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static class CollectionLine {
        private final String name;
        private final int quantity;

        private CollectionLine(String name, int quantity) {
            this.name = name;
            this.quantity = quantity;
        }

        private String name() {
            return name;
        }

        private int quantity() {
            return quantity;
        }
    }
}
