package com.mtg.repository;

import com.mtg.model.RecommendationBenchmarkAiJob;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecommendationBenchmarkAiJobRepository implements PanacheRepository<RecommendationBenchmarkAiJob> {
    public RecommendationBenchmarkAiJob latest() {
        return find("order by startedAt desc").firstResult();
    }

    public boolean hasRunning() {
        return count("status", "running") > 0;
    }
}
