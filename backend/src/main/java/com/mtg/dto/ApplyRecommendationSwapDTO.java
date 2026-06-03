package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ApplyRecommendationSwapDTO {
    private String add;
    private String remove;
    private String recommendationId;
    private String source;
    private String confidence;
    private String problem;
    private String risk;
    private String impactSummary;

    public ApplyRecommendationSwapDTO() {
    }

    public ApplyRecommendationSwapDTO(String add, String remove) {
        this(add, remove, null, null, null, null, null, null);
    }

    public ApplyRecommendationSwapDTO(
            String add,
            String remove,
            String recommendationId,
            String source,
            String confidence,
            String problem,
            String risk,
            String impactSummary
    ) {
        this.add = add;
        this.remove = remove;
        this.recommendationId = recommendationId;
        this.source = source;
        this.confidence = confidence;
        this.problem = problem;
        this.risk = risk;
        this.impactSummary = impactSummary;
    }

    public String add() { return add; }
    public void setAdd(String add) { this.add = add; }
    public String remove() { return remove; }
    public void setRemove(String remove) { this.remove = remove; }
    public String recommendationId() { return recommendationId; }
    public void setRecommendationId(String recommendationId) { this.recommendationId = recommendationId; }
    public String source() { return source; }
    public void setSource(String source) { this.source = source; }
    public String confidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public String problem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }
    public String risk() { return risk; }
    public void setRisk(String risk) { this.risk = risk; }
    public String impactSummary() { return impactSummary; }
    public void setImpactSummary(String impactSummary) { this.impactSummary = impactSummary; }
}
