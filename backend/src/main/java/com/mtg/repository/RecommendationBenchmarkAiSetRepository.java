package com.mtg.repository;

import com.mtg.model.RecommendationBenchmarkAiSet;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecommendationBenchmarkAiSetRepository implements PanacheRepository<RecommendationBenchmarkAiSet> {
    public RecommendationBenchmarkAiSet latestPromoted() {
        return find("status = 'promoted' order by promotedAt desc").firstResult();
    }
}
