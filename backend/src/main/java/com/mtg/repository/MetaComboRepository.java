package com.mtg.repository;

import com.mtg.model.MetaCombo;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class MetaComboRepository implements PanacheRepository<MetaCombo> {
    public MetaCombo findBySourceAndExternalId(String source, String externalId) {
        if (source == null || source.isBlank() || externalId == null || externalId.isBlank()) {
            return null;
        }
        return find("source = ?1 and externalId = ?2", source, externalId).firstResult();
    }

    public List<MetaCombo> listUsableCombos() {
        return list("order by coalesce(popularity, 0) desc, id asc");
    }
}
