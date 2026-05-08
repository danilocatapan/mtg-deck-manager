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

    @Column(name = "zone")
    private String zone = "main";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    private Deck deck;

    

    public DeckCard() {
    }

    public DeckCard(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
        this.zone = "main";
    }

    public DeckCard(String name, int quantity, String zone) {
        this.name = name;
        this.quantity = quantity;
        this.zone = zone;
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

    public String getZone() {
        return zone == null || zone.isBlank() ? "main" : zone;
    }

    public void setZone(String zone) {
        this.zone = zone == null || zone.isBlank() ? "main" : zone;
    }

    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }
}
