package com.mtg.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meta_decks")
public class MetaDeckSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "external_id", nullable = false, length = 300)
    private String externalId;

    @Column(nullable = false, length = 120)
    private String commander;

    @Column(name = "commander_normalized", nullable = false, length = 120)
    private String commanderNormalized;

    @Column(name = "color_identity", length = 8)
    private String colorIdentity;

    @Column(nullable = false, length = 32)
    private String bracket;

    @Column(name = "event_name", length = 300)
    private String eventName;

    @Column(name = "event_date")
    private LocalDate eventDate;

    private Integer placement;

    @Column(name = "player_count")
    private Integer playerCount;

    @Column(length = 1000)
    private String url;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MetaDeckSnapshotCard> cards = new ArrayList<>();

    public Long getId() { return id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getCommander() { return commander; }
    public void setCommander(String commander) { this.commander = commander; }
    public String getCommanderNormalized() { return commanderNormalized; }
    public void setCommanderNormalized(String commanderNormalized) { this.commanderNormalized = commanderNormalized; }
    public String getColorIdentity() { return colorIdentity; }
    public void setColorIdentity(String colorIdentity) { this.colorIdentity = colorIdentity; }
    public String getBracket() { return bracket; }
    public void setBracket(String bracket) { this.bracket = bracket; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public Integer getPlacement() { return placement; }
    public void setPlacement(Integer placement) { this.placement = placement; }
    public Integer getPlayerCount() { return playerCount; }
    public void setPlayerCount(Integer playerCount) { this.playerCount = playerCount; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public OffsetDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(OffsetDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
    public List<MetaDeckSnapshotCard> getCards() { return cards; }

    public void setCards(List<MetaDeckSnapshotCard> cards) {
        this.cards.clear();
        if (cards == null) return;
        for (MetaDeckSnapshotCard card : cards) {
            if (card != null) {
                card.setDeck(this);
                this.cards.add(card);
            }
        }
    }
}
