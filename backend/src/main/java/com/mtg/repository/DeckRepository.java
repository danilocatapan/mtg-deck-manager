package com.mtg.repository;

import com.mtg.model.Deck;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class DeckRepository implements PanacheRepository<Deck> {

    public Deck findByIdOrThrow(Long id) {
        return findById(id);
    }

    public List<Deck> listByOwner(String ownerId) {
        return list("ownerId", ownerId);
    }

    public Deck findByIdAndOwner(Long id, String ownerId) {
        return find("id = ?1 and ownerId = ?2", id, ownerId).firstResult();
    }
}
