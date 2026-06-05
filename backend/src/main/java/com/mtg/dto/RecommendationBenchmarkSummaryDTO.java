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
    private int evaluatedCases;
    private int humanReviewedCases;
    private java.util.Map<String, Long> feedback;
    private List<RecommendationBenchmarkNextActionDTO> nextActions;

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
        this.feedback = java.util.Map.of();
        this.nextActions = List.of();
    }

    public RecommendationBenchmarkSummaryDTO(
            String status,
            String baselineStatus,
            int totalCases,
            int targetCases,
            List<RecommendationBenchmarkCoverageDTO> coverage,
            List<RecommendationBenchmarkMetricDTO> metrics,
            List<String> limitations,
            int evaluatedCases,
            int humanReviewedCases,
            java.util.Map<String, Long> feedback,
            List<RecommendationBenchmarkNextActionDTO> nextActions
    ) {
        this(status, baselineStatus, totalCases, targetCases, coverage, metrics, limitations);
        this.evaluatedCases = evaluatedCases;
        this.humanReviewedCases = humanReviewedCases;
        this.feedback = feedback == null ? java.util.Map.of() : java.util.Map.copyOf(feedback);
        this.nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
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

    public int getEvaluatedCases() { return evaluatedCases; }
    public void setEvaluatedCases(int evaluatedCases) { this.evaluatedCases = evaluatedCases; }
    public int getHumanReviewedCases() { return humanReviewedCases; }
    public void setHumanReviewedCases(int humanReviewedCases) { this.humanReviewedCases = humanReviewedCases; }
    public java.util.Map<String, Long> getFeedback() { return feedback == null ? java.util.Map.of() : feedback; }
    public void setFeedback(java.util.Map<String, Long> feedback) { this.feedback = feedback; }
    public List<RecommendationBenchmarkNextActionDTO> getNextActions() { return nextActions == null ? List.of() : nextActions; }
    public void setNextActions(List<RecommendationBenchmarkNextActionDTO> nextActions) { this.nextActions = nextActions; }
}
