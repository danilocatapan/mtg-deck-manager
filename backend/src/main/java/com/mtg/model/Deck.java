package com.mtg.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    @Column(name = "author_display_name")
    private String authorDisplayName;

    @Column(name = "color_identity")
    private String colorIdentity;

    @Column(name = "visibility", nullable = false, length = 16)
    private DeckVisibility visibility = DeckVisibility.PRIVATE;

    @Column(name = "commanders_json", length = 2000)
    private String commandersJson;

    @Column(name = "history_json", columnDefinition = "TEXT")
    private String historyJson;

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

    public String getAuthorDisplayName() { return authorDisplayName; }

    public void setAuthorDisplayName(String authorDisplayName) { this.authorDisplayName = authorDisplayName; }

    public String getColorIdentity() { return colorIdentity; }

    public void setColorIdentity(String colorIdentity) { this.colorIdentity = colorIdentity; }

    public DeckVisibility getVisibility() { return visibility == null ? DeckVisibility.PRIVATE : visibility; }

    public void setVisibility(DeckVisibility visibility) { this.visibility = visibility == null ? DeckVisibility.PRIVATE : visibility; }

    public String getCommandersJson() { return commandersJson; }

    public void setCommandersJson(String commandersJson) { this.commandersJson = commandersJson; }

    public String getHistoryJson() { return historyJson; }

    public void setHistoryJson(String historyJson) { this.historyJson = historyJson; }

    public List<DeckCard> getCards() {
        return cards;
    }

    public void setCards(List<DeckCard> cards) {
        if (cards == null || cards.isEmpty()) {
            new ArrayList<>(this.cards).forEach(card -> {
                card.setDeck(null);
                this.cards.remove(card);
            });
            return;
        }

        List<String> requestedNames = new ArrayList<>();
        for (DeckCard incoming : cards) {
            if (incoming == null) {
                continue;
            }
            String normalizedName = normalizeCardName(incoming.getName());
            requestedNames.add(normalizedName);

            DeckCard existing = this.cards.stream()
                    .filter(card -> normalizeCardName(card.getName()).equals(normalizedName))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                existing.setName(incoming.getName());
                existing.setQuantity(incoming.getQuantity());
            } else {
                incoming.setDeck(this);
                this.cards.add(incoming);
            }
        }

        new ArrayList<>(this.cards).stream()
                .filter(card -> !requestedNames.contains(normalizeCardName(card.getName())))
                .forEach(card -> {
                    card.setDeck(null);
                    this.cards.remove(card);
                });
    }

    private String normalizeCardName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
