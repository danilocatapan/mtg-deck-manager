package com.mtg.repository;

import com.mtg.model.RecommendationAuditRun;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class RecommendationAuditRepository implements PanacheRepository<RecommendationAuditRun> {

    public List<RecommendationAuditRun> listByDeckAndOwner(Long deckId, String ownerId) {
        return list("deckId = ?1 and ownerId = ?2 order by createdAt desc", deckId, ownerId);
    }

    public RecommendationAuditRun findByIdAndOwner(Long id, String ownerId) {
        return find("id = ?1 and ownerId = ?2", id, ownerId).firstResult();
    }

    public List<RecommendationAuditRun> listByOwner(String ownerId) {
        return list("ownerId = ?1 order by createdAt desc", ownerId);
    }
}
