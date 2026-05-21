package com.mtg.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meta_top_decks")
public class MetaTopDeck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MetaDeckSource source;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "deck_url", length = 1000)
    private String deckUrl;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MetaDeckFormat format;

    @Column(nullable = false, length = 120)
    private String commander;

    @Column(name = "commander_normalized", nullable = false, length = 120)
    private String commanderNormalized;

    @Column(name = "deck_rank", nullable = false)
    private int rank;

    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_period", nullable = false, length = 32)
    private MetaRankingPeriod rankingPeriod;

    @Column(name = "ranking_date", nullable = false)
    private LocalDate rankingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MetaDeckArchetype archetype = MetaDeckArchetype.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MetaDeckBracket bracket = MetaDeckBracket.UNKNOWN;

    @Column(name = "color_identity", length = 8)
    private String colorIdentity;

    private Integer wins;

    private Integer losses;

    @Column(name = "popularity_score")
    private Double popularityScore;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "topDeck", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MetaTopDeckCard> cards = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "public_deck_id")
    private Deck publicDeck;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MetaDeckSource getSource() { return source; }
    public void setSource(MetaDeckSource source) { this.source = source; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getDeckUrl() { return deckUrl; }
    public void setDeckUrl(String deckUrl) { this.deckUrl = deckUrl; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public MetaDeckFormat getFormat() { return format; }
    public void setFormat(MetaDeckFormat format) { this.format = format; }
    public String getCommander() { return commander; }
    public void setCommander(String commander) { this.commander = commander; }
    public String getCommanderNormalized() { return commanderNormalized; }
    public void setCommanderNormalized(String commanderNormalized) { this.commanderNormalized = commanderNormalized; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public MetaRankingPeriod getRankingPeriod() { return rankingPeriod; }
    public void setRankingPeriod(MetaRankingPeriod rankingPeriod) { this.rankingPeriod = rankingPeriod; }
    public LocalDate getRankingDate() { return rankingDate; }
    public void setRankingDate(LocalDate rankingDate) { this.rankingDate = rankingDate; }
    public MetaDeckArchetype getArchetype() { return archetype; }
    public void setArchetype(MetaDeckArchetype archetype) { this.archetype = archetype; }
    public MetaDeckBracket getBracket() { return bracket; }
    public void setBracket(MetaDeckBracket bracket) { this.bracket = bracket; }
    public String getColorIdentity() { return colorIdentity; }
    public void setColorIdentity(String colorIdentity) { this.colorIdentity = colorIdentity; }
    public Integer getWins() { return wins; }
    public void setWins(Integer wins) { this.wins = wins; }
    public Integer getLosses() { return losses; }
    public void setLosses(Integer losses) { this.losses = losses; }
    public Double getPopularityScore() { return popularityScore; }
    public void setPopularityScore(Double popularityScore) { this.popularityScore = popularityScore; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<MetaTopDeckCard> getCards() { return cards; }
    public Deck getPublicDeck() { return publicDeck; }
    public void setPublicDeck(Deck publicDeck) { this.publicDeck = publicDeck; }

    public void setCards(List<MetaTopDeckCard> cards) {
        this.cards.clear();
        if (cards == null) {
            return;
        }
        for (MetaTopDeckCard card : cards) {
            if (card != null) {
                card.setTopDeck(this);
                this.cards.add(card);
            }
        }
    }
}
