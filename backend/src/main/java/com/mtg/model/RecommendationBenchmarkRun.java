package com.mtg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "recommendation_benchmark_runs")
public class RecommendationBenchmarkRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "fixture_version", nullable = false, length = 64)
    private String fixtureVersion;
    @Column(name = "baseline_version", nullable = false, length = 64)
    private String baselineVersion;
    @Column(name = "algorithm_version", nullable = false, length = 64)
    private String algorithmVersion;
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;
    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;
    @Column(name = "total_cases", nullable = false)
    private int totalCases;
    @Column(name = "evaluated_cases", nullable = false)
    private int evaluatedCases;
    @Column(name = "metrics_json", columnDefinition = "TEXT")
    private String metricsJson;
    @Column(name = "error_code", length = 120)
    private String errorCode;

    public Long getId() { return id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFixtureVersion() { return fixtureVersion; }
    public void setFixtureVersion(String fixtureVersion) { this.fixtureVersion = fixtureVersion; }
    public String getBaselineVersion() { return baselineVersion; }
    public void setBaselineVersion(String baselineVersion) { this.baselineVersion = baselineVersion; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
    public int getTotalCases() { return totalCases; }
    public void setTotalCases(int totalCases) { this.totalCases = totalCases; }
    public int getEvaluatedCases() { return evaluatedCases; }
    public void setEvaluatedCases(int evaluatedCases) { this.evaluatedCases = evaluatedCases; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}
