package com.mtg.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decks")
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String commander;

    @Column(name = "owner_id")
    private String ownerId;

    @Column(name = "color_identity")
    private String colorIdentity;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DeckCard> cards = new ArrayList<>();

    public Deck() {
    }

    public Deck(String name, String commander, List<DeckCard> cards) {
        this.name = name;
        this.commander = commander;
        if (cards != null) {
            cards.forEach(c -> c.setDeck(this));
            this.cards = new ArrayList<>(cards);
        }
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

    public String getCommander() {
        return commander;
    }

    public void setCommander(String commander) {
        this.commander = commander;
    }

    public String getOwnerId() { return ownerId; }

    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getColorIdentity() { return colorIdentity; }

    public void setColorIdentity(String colorIdentity) { this.colorIdentity = colorIdentity; }

    public List<DeckCard> getCards() {
        return cards;
    }

    public void setCards(List<DeckCard> cards) {
        this.cards.clear();
        if (cards != null) {
            cards.forEach(c -> c.setDeck(this));
            this.cards.addAll(cards);
        }
    }
}
