package com.mtg.repository;

import com.mtg.model.Deck;
import com.mtg.model.DeckVisibility;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class DeckRepository implements PanacheRepository<Deck> {
    private static final int DEFAULT_PUBLIC_PAGE_SIZE = 12;
    private static final int MAX_PUBLIC_PAGE_SIZE = 50;

    public Deck findByIdOrThrow(Long id) {
        return findById(id);
    }

    public List<Deck> listByOwner(String ownerId) {
        return list("ownerId", ownerId);
    }

    public Deck findByIdAndOwner(Long id, String ownerId) {
        return find("id = ?1 and ownerId = ?2", id, ownerId).firstResult();
    }

    public List<Deck> listPublic(Integer page, Integer size, String commander) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null ? DEFAULT_PUBLIC_PAGE_SIZE : Math.max(1, Math.min(size, MAX_PUBLIC_PAGE_SIZE));
        if (commander != null && !commander.isBlank()) {
            String filter = "%" + commander.trim().toLowerCase(Locale.ROOT) + "%";
            return find("visibility = ?1 and lower(commander) like ?2 order by id desc", DeckVisibility.PUBLIC, filter)
                    .page(Page.of(safePage, safeSize))
                    .list();
        }
        return find("visibility = ?1 order by id desc", DeckVisibility.PUBLIC)
                .page(Page.of(safePage, safeSize))
                .list();
    }

    public Deck findPublicByIdOrOwner(Long id, String ownerId) {
        return find("id = ?1 and (visibility = ?2 or ownerId = ?3)", id, DeckVisibility.PUBLIC, ownerId).firstResult();
    }
}
