package com.mtg.dto;

import java.util.List;

public class RecommendationBenchmarkSummaryDTO {
    private String status;
    private String baselineStatus;
    private int totalCases;
    private int targetCases;
    private List<RecommendationBenchmarkCoverageDTO> coverage;
    private List<RecommendationBenchmarkMetricDTO> metrics;
    private List<String> limitations;

    public RecommendationBenchmarkSummaryDTO() {
    }

    public RecommendationBenchmarkSummaryDTO(
            String status,
            String baselineStatus,
            int totalCases,
            int targetCases,
            List<RecommendationBenchmarkCoverageDTO> coverage,
            List<RecommendationBenchmarkMetricDTO> metrics,
            List<String> limitations
    ) {
        this.status = status;
        this.baselineStatus = baselineStatus;
        this.totalCases = totalCases;
        this.targetCases = targetCases;
        this.coverage = coverage == null ? List.of() : List.copyOf(coverage);
        this.metrics = metrics == null ? List.of() : List.copyOf(metrics);
        this.limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getBaselineStatus() { return baselineStatus; }

    public void setBaselineStatus(String baselineStatus) { this.baselineStatus = baselineStatus; }

    public int getTotalCases() { return totalCases; }

    public void setTotalCases(int totalCases) { this.totalCases = totalCases; }

    public int getTargetCases() { return targetCases; }

    public void setTargetCases(int targetCases) { this.targetCases = targetCases; }

    public List<RecommendationBenchmarkCoverageDTO> getCoverage() { return coverage == null ? List.of() : coverage; }

    public void setCoverage(List<RecommendationBenchmarkCoverageDTO> coverage) { this.coverage = coverage; }

    public List<RecommendationBenchmarkMetricDTO> getMetrics() { return metrics == null ? List.of() : metrics; }

    public void setMetrics(List<RecommendationBenchmarkMetricDTO> metrics) { this.metrics = metrics; }

    public List<String> getLimitations() { return limitations == null ? List.of() : limitations; }

    public void setLimitations(List<String> limitations) { this.limitations = limitations; }
}
