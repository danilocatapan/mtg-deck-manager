package com.mtg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "meta_top_deck_cards")
public class MetaTopDeckCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "top_deck_id", nullable = false)
    private MetaTopDeck topDeck;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 120)
    private String nameNormalized;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MetaDeckCardSection section;

    @Column(name = "scryfall_id", length = 80)
    private String scryfallId;

    @Column(name = "set_code", length = 16)
    private String setCode;

    @Column(name = "set_name", length = 160)
    private String setName;

    @Column(name = "collector_number", length = 32)
    private String collectorNumber;

    @Column(length = 16)
    private String finish;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MetaTopDeck getTopDeck() { return topDeck; }
    public void setTopDeck(MetaTopDeck topDeck) { this.topDeck = topDeck; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameNormalized() { return nameNormalized; }
    public void setNameNormalized(String nameNormalized) { this.nameNormalized = nameNormalized; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public MetaDeckCardSection getSection() { return section; }
    public void setSection(MetaDeckCardSection section) { this.section = section; }
    public String getScryfallId() { return scryfallId; }
    public void setScryfallId(String scryfallId) { this.scryfallId = scryfallId; }
    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }
    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }
    public String getCollectorNumber() { return collectorNumber; }
    public void setCollectorNumber(String collectorNumber) { this.collectorNumber = collectorNumber; }
    public String getFinish() { return finish; }
    public void setFinish(String finish) { this.finish = finish; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
