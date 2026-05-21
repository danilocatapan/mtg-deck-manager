package com.mtg.repository;

import com.mtg.model.MetaTopDeckImportBatch;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MetaTopDeckImportBatchRepository implements PanacheRepository<MetaTopDeckImportBatch> {
}
