package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RecommendationAuditFeedbackDTO {
    private String status;
    private String reason;
    private String notes;

    public RecommendationAuditFeedbackDTO() {
    }

    public RecommendationAuditFeedbackDTO(String status, String reason, String notes) {
        this.status = status;
        this.reason = reason;
        this.notes = notes;
    }

    public String status() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String reason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String notes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
