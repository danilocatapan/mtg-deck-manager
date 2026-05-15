package com.mtg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "recommendation_audit_runs")
public class RecommendationAuditRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deck_id")
    private Long deckId;

    @Column(name = "owner_id")
    private String ownerId;

    private String commander;

    @Column(name = "color_identity")
    private String colorIdentity;

    private String bracket;

    private String archetype;

    @Column(name = "algorithm_version")
    private String algorithmVersion;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Lob
    @Column(name = "gaps_json", columnDefinition = "TEXT")
    private String gapsJson;

    @Lob
    @Column(name = "issues_json", columnDefinition = "TEXT")
    private String issuesJson;

    @Lob
    @Column(name = "weak_cards_json", columnDefinition = "TEXT")
    private String weakCardsJson;

    @Lob
    @Column(name = "params_json", columnDefinition = "TEXT")
    private String paramsJson;

    @Lob
    @Column(name = "recommendations_json", columnDefinition = "TEXT")
    private String recommendationsJson;

    @Lob
    @Column(name = "blocked_pairs_json", columnDefinition = "TEXT")
    private String blockedPairsJson;

    @Lob
    @Column(name = "protected_cuts_json", columnDefinition = "TEXT")
    private String protectedCutsJson;

    @Column(name = "feedback_status")
    private String feedbackStatus;

    @Column(name = "feedback_reason")
    private String feedbackReason;

    @Lob
    @Column(name = "feedback_notes", columnDefinition = "TEXT")
    private String feedbackNotes;

    @Column(name = "feedback_at")
    private OffsetDateTime feedbackAt;

    public Long getId() { return id; }
    public Long getDeckId() { return deckId; }
    public void setDeckId(Long deckId) { this.deckId = deckId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getCommander() { return commander; }
    public void setCommander(String commander) { this.commander = commander; }
    public String getColorIdentity() { return colorIdentity; }
    public void setColorIdentity(String colorIdentity) { this.colorIdentity = colorIdentity; }
    public String getBracket() { return bracket; }
    public void setBracket(String bracket) { this.bracket = bracket; }
    public String getArchetype() { return archetype; }
    public void setArchetype(String archetype) { this.archetype = archetype; }
    public String getAlgorithmVersion() { return algorithmVersion; }
    public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getGapsJson() { return gapsJson; }
    public void setGapsJson(String gapsJson) { this.gapsJson = gapsJson; }
    public String getIssuesJson() { return issuesJson; }
    public void setIssuesJson(String issuesJson) { this.issuesJson = issuesJson; }
    public String getWeakCardsJson() { return weakCardsJson; }
    public void setWeakCardsJson(String weakCardsJson) { this.weakCardsJson = weakCardsJson; }
    public String getParamsJson() { return paramsJson; }
    public void setParamsJson(String paramsJson) { this.paramsJson = paramsJson; }
    public String getRecommendationsJson() { return recommendationsJson; }
    public void setRecommendationsJson(String recommendationsJson) { this.recommendationsJson = recommendationsJson; }
    public String getBlockedPairsJson() { return blockedPairsJson; }
    public void setBlockedPairsJson(String blockedPairsJson) { this.blockedPairsJson = blockedPairsJson; }
    public String getProtectedCutsJson() { return protectedCutsJson; }
    public void setProtectedCutsJson(String protectedCutsJson) { this.protectedCutsJson = protectedCutsJson; }
    public String getFeedbackStatus() { return feedbackStatus; }
    public void setFeedbackStatus(String feedbackStatus) { this.feedbackStatus = feedbackStatus; }
    public String getFeedbackReason() { return feedbackReason; }
    public void setFeedbackReason(String feedbackReason) { this.feedbackReason = feedbackReason; }
    public String getFeedbackNotes() { return feedbackNotes; }
    public void setFeedbackNotes(String feedbackNotes) { this.feedbackNotes = feedbackNotes; }
    public OffsetDateTime getFeedbackAt() { return feedbackAt; }
    public void setFeedbackAt(OffsetDateTime feedbackAt) { this.feedbackAt = feedbackAt; }
}
