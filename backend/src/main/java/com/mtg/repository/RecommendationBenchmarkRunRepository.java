package com.mtg.repository;

import com.mtg.model.RecommendationBenchmarkRun;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecommendationBenchmarkRunRepository implements PanacheRepository<RecommendationBenchmarkRun> {
    public RecommendationBenchmarkRun latestSuccessful() {
        return find("status = 'success' order by finishedAt desc").firstResult();
    }

    public boolean hasRunning() {
        return count("status", "running") > 0;
    }
}
