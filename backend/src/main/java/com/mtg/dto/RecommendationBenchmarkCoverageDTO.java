package com.mtg.dto;

public class RecommendationBenchmarkCoverageDTO {
    private String commander;
    private String bracket;
    private int cases;
    private String statusAgainstGpt;

    public RecommendationBenchmarkCoverageDTO() {
    }

    public RecommendationBenchmarkCoverageDTO(String commander, String bracket, int cases, String statusAgainstGpt) {
        this.commander = commander;
        this.bracket = bracket;
        this.cases = cases;
        this.statusAgainstGpt = statusAgainstGpt;
    }

    public String getCommander() { return commander; }

    public void setCommander(String commander) { this.commander = commander; }

    public String getBracket() { return bracket; }

    public void setBracket(String bracket) { this.bracket = bracket; }

    public int getCases() { return cases; }

    public void setCases(int cases) { this.cases = cases; }

    public String getStatusAgainstGpt() { return statusAgainstGpt; }

    public void setStatusAgainstGpt(String statusAgainstGpt) { this.statusAgainstGpt = statusAgainstGpt; }
}
