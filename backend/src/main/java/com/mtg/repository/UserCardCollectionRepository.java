package com.mtg.repository;

import com.mtg.model.UserCardCollectionItem;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class UserCardCollectionRepository implements PanacheRepository<UserCardCollectionItem> {
    public List<UserCardCollectionItem> listByOwner(String ownerId) {
        return list("ownerId = ?1 order by cardName", ownerId);
    }

    public UserCardCollectionItem findByOwnerAndCard(String ownerId, String normalizedName) {
        return find("ownerId = ?1 and cardNameNormalized = ?2", ownerId, normalizedName).firstResult();
    }

    public long countByOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            return 0;
        }
        return count("ownerId", ownerId);
    }

    public long deleteByOwner(String ownerId) {
        return delete("ownerId", ownerId);
    }
}
