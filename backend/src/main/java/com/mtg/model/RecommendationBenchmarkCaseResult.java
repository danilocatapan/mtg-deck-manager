package com.mtg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recommendation_benchmark_case_results")
public class RecommendationBenchmarkCaseResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false)
    private Long runId;
    @Column(name = "case_id", nullable = false, length = 120)
    private String caseId;
    @Column(nullable = false, length = 120)
    private String commander;
    @Column(nullable = false, length = 32)
    private String bracket;
    @Column(name = "system_output_json", nullable = false, columnDefinition = "TEXT")
    private String systemOutputJson;
    @Column(name = "gpt_output_json", nullable = false, columnDefinition = "TEXT")
    private String gptOutputJson;
    @Column(name = "option_a_json", nullable = false, columnDefinition = "TEXT")
    private String optionAJson;
    @Column(name = "option_b_json", nullable = false, columnDefinition = "TEXT")
    private String optionBJson;
    @Column(name = "system_option", nullable = false, length = 1)
    private String systemOption;
    @Column(name = "metrics_json", nullable = false, columnDefinition = "TEXT")
    private String metricsJson;

    public Long getId() { return id; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public String getCommander() { return commander; }
    public void setCommander(String commander) { this.commander = commander; }
    public String getBracket() { return bracket; }
    public void setBracket(String bracket) { this.bracket = bracket; }
    public String getSystemOutputJson() { return systemOutputJson; }
    public void setSystemOutputJson(String systemOutputJson) { this.systemOutputJson = systemOutputJson; }
    public String getGptOutputJson() { return gptOutputJson; }
    public void setGptOutputJson(String gptOutputJson) { this.gptOutputJson = gptOutputJson; }
    public String getOptionAJson() { return optionAJson; }
    public void setOptionAJson(String optionAJson) { this.optionAJson = optionAJson; }
    public String getOptionBJson() { return optionBJson; }
    public void setOptionBJson(String optionBJson) { this.optionBJson = optionBJson; }
    public String getSystemOption() { return systemOption; }
    public void setSystemOption(String systemOption) { this.systemOption = systemOption; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
}
