package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MetaTopDeckImportDeckDTO {
    private Integer rank;
    private String name;
    private String commander;
    private String deckUrl;
    private String decklist;
    private String archetype;
    private String bracket;
    private List<String> colorIdentity;
    private Integer wins;
    private Integer losses;
    private Double popularityScore;
    private List<MetaTopDeckCardRequestDTO> cards;

    public MetaTopDeckImportDeckDTO() {
    }

    public MetaTopDeckImportDeckDTO(
            Integer rank,
            String name,
            String commander,
            String deckUrl,
            String decklist,
            String archetype,
            String bracket,
            List<String> colorIdentity,
            Integer wins,
            Integer losses,
            Double popularityScore,
            List<MetaTopDeckCardRequestDTO> cards
    ) {
        this.rank = rank;
        this.name = name;
        this.commander = commander;
        this.deckUrl = deckUrl;
        this.decklist = decklist;
        this.archetype = archetype;
        this.bracket = bracket;
        this.colorIdentity = colorIdentity;
        this.wins = wins;
        this.losses = losses;
        this.popularityScore = popularityScore;
        this.cards = cards;
    }

    public Integer rank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public String commander() { return commander; }
    public void setCommander(String commander) { this.commander = commander; }
    public String deckUrl() { return deckUrl; }
    public void setDeckUrl(String deckUrl) { this.deckUrl = deckUrl; }
    public String decklist() { return decklist; }
    public void setDecklist(String decklist) { this.decklist = decklist; }
    public String archetype() { return archetype; }
    public void setArchetype(String archetype) { this.archetype = archetype; }
    public String bracket() { return bracket; }
    public void setBracket(String bracket) { this.bracket = bracket; }
    public List<String> colorIdentity() { return colorIdentity; }
    public void setColorIdentity(List<String> colorIdentity) { this.colorIdentity = colorIdentity; }
    public Integer wins() { return wins; }
    public void setWins(Integer wins) { this.wins = wins; }
    public Integer losses() { return losses; }
    public void setLosses(Integer losses) { this.losses = losses; }
    public Double popularityScore() { return popularityScore; }
    public void setPopularityScore(Double popularityScore) { this.popularityScore = popularityScore; }
    public List<MetaTopDeckCardRequestDTO> cards() { return cards; }
    public void setCards(List<MetaTopDeckCardRequestDTO> cards) { this.cards = cards; }
}
