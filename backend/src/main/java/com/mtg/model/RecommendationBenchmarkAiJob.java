package com.mtg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "recommendation_benchmark_ai_jobs")
public class RecommendationBenchmarkAiJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 32) private String status;
    @Column(nullable = false, length = 64) private String model;
    @Column(name = "fixture_version", nullable = false, length = 64) private String fixtureVersion;
    @Column(name = "algorithm_version", nullable = false, length = 64) private String algorithmVersion;
    @Column(name = "prompt_version", nullable = false, length = 64) private String promptVersion;
    @Column(name = "total_calls", nullable = false) private int totalCalls;
    @Column(name = "completed_calls", nullable = false) private int completedCalls;
    @Column(name = "failed_calls", nullable = false) private int failedCalls;
    @Column(name = "started_at", nullable = false) private OffsetDateTime startedAt;
    @Column(name = "finished_at") private OffsetDateTime finishedAt;
    @Column(name = "error_code", length = 120) private String errorCode;

    public Long getId() { return id; }
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
    public int getTotalCalls() { return totalCalls; }
    public void setTotalCalls(int totalCalls) { this.totalCalls = totalCalls; }
    public int getCompletedCalls() { return completedCalls; }
    public void setCompletedCalls(int completedCalls) { this.completedCalls = completedCalls; }
    public int getFailedCalls() { return failedCalls; }
    public void setFailedCalls(int failedCalls) { this.failedCalls = failedCalls; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}
