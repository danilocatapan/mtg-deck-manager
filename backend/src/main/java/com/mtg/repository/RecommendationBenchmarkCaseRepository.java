package com.mtg.repository;

import com.mtg.model.RecommendationBenchmarkCaseResult;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class RecommendationBenchmarkCaseRepository implements PanacheRepository<RecommendationBenchmarkCaseResult> {
    public List<RecommendationBenchmarkCaseResult> byRun(Long runId) {
        return list("runId = ?1 order by caseId", runId);
    }

    public RecommendationBenchmarkCaseResult byRunAndCase(Long runId, String caseId) {
        return find("runId = ?1 and caseId = ?2", runId, caseId).firstResult();
    }
}
