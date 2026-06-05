package com.mtg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "recommendation_benchmark_ai_sets")
public class RecommendationBenchmarkAiSet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "job_id", nullable = false) private Long jobId;
    @Column(nullable = false, length = 32) private String status;
    @Column(nullable = false, length = 64) private String model;
    @Column(name = "fixture_version", nullable = false, length = 64) private String fixtureVersion;
    @Column(name = "algorithm_version", nullable = false, length = 64) private String algorithmVersion;
    @Column(name = "prompt_version", nullable = false, length = 64) private String promptVersion;
    @Column(name = "total_cases", nullable = false) private int totalCases;
    @Column(name = "promoted_at") private OffsetDateTime promotedAt;
    @Column(name = "metrics_json", columnDefinition = "TEXT") private String metricsJson;

    public Long getId() { return id; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getFixtureVersion() { return fixtureVersion; }
    public void setFixtureVersion(String fixtureVersion) { this.fixtureVersion = fixtureVersion; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public int getTotalCases() { return totalCases; }
    public void setTotalCases(int totalCases) { this.totalCases = totalCases; }
    public OffsetDateTime getPromotedAt() { return promotedAt; }
    public void setPromotedAt(OffsetDateTime promotedAt) { this.promotedAt = promotedAt; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
}
