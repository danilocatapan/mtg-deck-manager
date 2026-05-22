package com.mtg.model;

import jakarta.persistence.*;

@Entity
@Table(name = "deck_cards")
public class DeckCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int quantity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    private Deck deck;

    

    public DeckCard() {
    }

    public DeckCard(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    public DeckCard(String name, int quantity, String setCode, String collectorNumber, String finish) {
        this.name = name;
        this.quantity = quantity;
        this.setCode = setCode;
        this.collectorNumber = collectorNumber;
        this.finish = finish;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getScryfallId() {
        return scryfallId;
    }

    public void setScryfallId(String scryfallId) {
        this.scryfallId = scryfallId;
    }

    public String getSetCode() {
        return setCode;
    }

    public void setSetCode(String setCode) {
        this.setCode = setCode;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public String getCollectorNumber() {
        return collectorNumber;
    }

    public void setCollectorNumber(String collectorNumber) {
        this.collectorNumber = collectorNumber;
    }

    public String getFinish() {
        return finish;
    }

    public void setFinish(String finish) {
        this.finish = finish;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }
}
