package com.mtg.repository;

import com.mtg.model.Deck;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeckRepository implements PanacheRepository<Deck> {

    public Deck findByIdOrThrow(Long id) {
        return findById(id);
    }
}
