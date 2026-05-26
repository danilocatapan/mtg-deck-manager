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
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meta_combos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_meta_combos_source_external_id", columnNames = {"source", "external_id"})
})
public class MetaCombo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(name = "external_id", nullable = false, length = 120)
    private String externalId;

    @Column(nullable = false, length = 320)
    private String name;

    @Column(name = "result_text", length = 2000)
    private String resultText;

    @Column(length = 1000)
    private String tags;

    @Column(length = 1000)
    private String legalities;

    @Column(length = 1000)
    private String brackets;

    @Column(name = "commander_required", length = 160)
    private String commanderRequired;

    private Integer popularity;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "source_updated_at")
    private OffsetDateTime sourceUpdatedAt;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt;

    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MetaComboCard> cards = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getResultText() { return resultText; }
    public void setResultText(String resultText) { this.resultText = resultText; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getLegalities() { return legalities; }
    public void setLegalities(String legalities) { this.legalities = legalities; }
    public String getBrackets() { return brackets; }
    public void setBrackets(String brackets) { this.brackets = brackets; }
    public String getCommanderRequired() { return commanderRequired; }
    public void setCommanderRequired(String commanderRequired) { this.commanderRequired = commanderRequired; }
    public Integer getPopularity() { return popularity; }
    public void setPopularity(Integer popularity) { this.popularity = popularity; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public OffsetDateTime getSourceUpdatedAt() { return sourceUpdatedAt; }
    public void setSourceUpdatedAt(OffsetDateTime sourceUpdatedAt) { this.sourceUpdatedAt = sourceUpdatedAt; }
    public OffsetDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(OffsetDateTime syncedAt) { this.syncedAt = syncedAt; }
    public List<MetaComboCard> getCards() { return cards; }

    public void setCards(List<MetaComboCard> cards) {
        this.cards.clear();
        if (cards == null) {
            return;
        }
        for (MetaComboCard card : cards) {
            if (card != null) {
                card.setCombo(this);
                this.cards.add(card);
            }
        }
    }
}
