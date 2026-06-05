package com.mtg.service.meta;

import com.mtg.model.MetaDeckSnapshot;
import com.mtg.model.MetaDeckSnapshotCard;
import com.mtg.repository.MetaDeckSnapshotRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class MetaDatasetService {
    private static final Logger LOG = Logger.getLogger(MetaDatasetService.class);

    @Inject
    MetaDeckSnapshotRepository repository;

    @Transactional
    public void replaceBySource(String source, List<MetaDeck> importedDecks) {
        if (source == null || source.isBlank()) {
            return;
        }
        repository.deleteBySource(source);
        if (importedDecks != null && !importedDecks.isEmpty()) {
            repository.persist(importedDecks.stream().map(this::toEntity).toList());
        }
        LOG.infov("event=meta.dataset.replaced source={0} decks={1}", source, importedDecks == null ? 0 : importedDecks.size());
    }

    public List<MetaDeck> findAll() {
        return repository == null
                ? List.of()
                : repository.listAllOrdered().stream().map(this::toDomain).toList();
    }

    private MetaDeckSnapshot toEntity(MetaDeck deck) {
        MetaDeckSnapshot entity = new MetaDeckSnapshot();
        entity.setSource(deck.source());
        entity.setExternalId(deck.externalId());
        entity.setCommander(deck.commander());
        entity.setCommanderNormalized(normalize(deck.commander()));
        entity.setColorIdentity(String.join("", deck.colorIdentity()));
        entity.setBracket(deck.bracket());
        entity.setEventName(deck.eventName());
        entity.setEventDate(deck.eventDate());
        entity.setPlacement(deck.placement());
        entity.setPlayerCount(deck.playerCount());
        entity.setUrl(deck.url());
        entity.setFetchedAt(deck.fetchedAt());
        entity.setCards(deck.cards().stream().map(card -> {
            MetaDeckSnapshotCard persisted = new MetaDeckSnapshotCard();
            persisted.setName(card.name());
            persisted.setQuantity(Math.max(1, card.quantity()));
            return persisted;
        }).toList());
        return entity;
    }

    private MetaDeck toDomain(MetaDeckSnapshot entity) {
        return new MetaDeck(
                entity.getSource(),
                entity.getExternalId(),
                entity.getCommander(),
                List.of(),
                colorList(entity.getColorIdentity()),
                entity.getBracket(),
                List.of(),
                entity.getCards().stream()
                        .map(card -> new MetaDeckCard(card.getName(), card.getQuantity(), List.of(), null, null, List.of()))
                        .toList(),
                entity.getEventName(),
                entity.getEventDate(),
                entity.getPlacement(),
                entity.getPlayerCount(),
                entity.getUrl(),
                entity.getFetchedAt()
        );
    }

    private List<String> colorList(String colors) {
        if (colors == null || colors.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        colors.chars().mapToObj(value -> String.valueOf((char) value)).forEach(result::add);
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
