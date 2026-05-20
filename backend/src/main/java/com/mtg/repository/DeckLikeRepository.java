package com.mtg.repository;

import com.mtg.model.Deck;
import com.mtg.model.DeckLike;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class DeckLikeRepository implements PanacheRepository<DeckLike> {

    public DeckLike findByDeckAndOwner(Long deckId, String ownerId) {
        return find("deck.id = ?1 and ownerId = ?2", deckId, ownerId).firstResult();
    }

    public long countByDeck(Long deckId) {
        return count("deck.id", deckId);
    }

    public boolean existsByDeckAndOwner(Long deckId, String ownerId) {
        return ownerId != null && findByDeckAndOwner(deckId, ownerId) != null;
    }

    public long deleteByDeckAndOwner(Long deckId, String ownerId) {
        return delete("deck.id = ?1 and ownerId = ?2", deckId, ownerId);
    }

    public long deleteByDeck(Long deckId) {
        return delete("deck.id", deckId);
    }

    public long deleteByOwner(String ownerId) {
        return delete("ownerId", ownerId);
    }

    @SuppressWarnings("unchecked")
    public List<Deck> listTopPublicDecks(OffsetDateTime since, int limit) {
        return getEntityManager()
                .createQuery("""
                        select deck
                        from Deck deck
                        left join DeckLike deckLike
                            on deckLike.deck = deck and deckLike.createdAt >= :since
                        where deck.visibility = com.mtg.model.DeckVisibility.PUBLIC
                        group by deck
                        order by count(deckLike.id) desc, deck.id desc
                        """)
                .setParameter("since", since)
                .setMaxResults(limit)
                .getResultList();
    }
}
