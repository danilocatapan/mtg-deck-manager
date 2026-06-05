package com.mtg.dto;

public class RecommendationBenchmarkMetricDTO {
    private String name;
    private String value;
    private String target;
    private String status;
    private int sampleSize;

    public RecommendationBenchmarkMetricDTO() {
    }

    public RecommendationBenchmarkMetricDTO(String name, String value, String target, String status) {
        this(name, value, target, status, 0);
    }

    public RecommendationBenchmarkMetricDTO(String name, String value, String target, String status, int sampleSize) {
        this.name = name;
        this.value = value;
        this.target = target;
        this.status = status;
        this.sampleSize = sampleSize;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getValue() { return value; }

    public void setValue(String value) { this.value = value; }

    public String getTarget() { return target; }

    public void setTarget(String target) { this.target = target; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public int getSampleSize() { return sampleSize; }

    public void setSampleSize(int sampleSize) { this.sampleSize = sampleSize; }
}
