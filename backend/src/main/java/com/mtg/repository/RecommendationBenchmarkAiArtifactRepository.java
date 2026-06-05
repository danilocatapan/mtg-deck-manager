package com.mtg.repository;

import com.mtg.model.RecommendationBenchmarkAiArtifact;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class RecommendationBenchmarkAiArtifactRepository implements PanacheRepository<RecommendationBenchmarkAiArtifact> {
    public List<RecommendationBenchmarkAiArtifact> byJob(Long jobId) {
        return list("jobId = ?1 order by caseId, artifactType", jobId);
    }

    public RecommendationBenchmarkAiArtifact byJobCaseAndType(Long jobId, String caseId, String artifactType) {
        return find("jobId = ?1 and caseId = ?2 and artifactType = ?3", jobId, caseId, artifactType).firstResult();
    }
}
