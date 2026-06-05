package com.mtg.repository;

import com.mtg.model.MetaDeckSnapshot;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class MetaDeckSnapshotRepository implements PanacheRepository<MetaDeckSnapshot> {
    public List<MetaDeckSnapshot> listAllOrdered() {
        return list("order by fetchedAt desc, source, commander, placement");
    }

    public void deleteBySource(String source) {
        list("lower(source)", source.trim().toLowerCase(Locale.ROOT)).forEach(this::delete);
    }

    public void deleteSnapshots() {
        listAll().forEach(this::delete);
    }
}
