package com.mtg.repository;

import com.mtg.model.RecommendationBenchmarkReview;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class RecommendationBenchmarkReviewRepository implements PanacheRepository<RecommendationBenchmarkReview> {
    public RecommendationBenchmarkReview byReviewer(Long runId, String caseId, String reviewerId) {
        return find("runId = ?1 and caseId = ?2 and reviewerId = ?3", runId, caseId, reviewerId).firstResult();
    }

    public List<RecommendationBenchmarkReview> byRun(Long runId) {
        return list("runId", runId);
    }
}
