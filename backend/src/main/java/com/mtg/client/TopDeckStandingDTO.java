package com.mtg.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TopDeckStandingDTO {
    private String name;
    private String commander;
    private String decklist;
    private Map<String, Integer> deckObj;
    private Integer wins;
    private Integer draws;
    private Integer losses;
    private Double winRate;

    public TopDeckStandingDTO() {
    }

    public TopDeckStandingDTO(String name, String decklist, Integer wins, Integer draws, Integer losses, Double winRate) {
        this(name, null, decklist, null, wins, draws, losses, winRate);
    }

    public TopDeckStandingDTO(
            String name,
            String commander,
            String decklist,
            Map<String, Integer> deckObj,
            Integer wins,
            Integer draws,
            Integer losses,
            Double winRate
    ) {
        this.name = name;
        this.commander = commander;
        this.decklist = decklist;
        this.deckObj = deckObj;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.winRate = winRate;
    }

    public String name() { return name; }

    public void setName(String name) { this.name = name; }

    public String commander() { return commander; }

    public void setCommander(String commander) { this.commander = commander; }

    public String decklist() { return decklist; }

    public void setDecklist(String decklist) { this.decklist = decklist; }

    public Map<String, Integer> deckObj() { return deckObj; }

    public void setDeckObj(Map<String, Integer> deckObj) { this.deckObj = deckObj; }

    public Integer wins() { return wins; }

    public void setWins(Integer wins) { this.wins = wins; }

    public Integer draws() { return draws; }

    public void setDraws(Integer draws) { this.draws = draws; }

    public Integer losses() { return losses; }

    public void setLosses(Integer losses) { this.losses = losses; }

    public Double winRate() { return winRate; }

    public void setWinRate(Double winRate) { this.winRate = winRate; }
}
