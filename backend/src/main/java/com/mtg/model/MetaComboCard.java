package com.mtg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "meta_combo_cards")
public class MetaComboCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    private MetaCombo combo;

    @Column(name = "card_name", nullable = false, length = 180)
    private String cardName;

    @Column(name = "card_normalized", nullable = false, length = 180)
    private String cardNormalized;

    @Column(name = "card_role", length = 64)
    private String cardRole;

    @Column(name = "commander_slot", nullable = false)
    private boolean commanderSlot;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MetaCombo getCombo() { return combo; }
    public void setCombo(MetaCombo combo) { this.combo = combo; }
    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }
    public String getCardNormalized() { return cardNormalized; }
    public void setCardNormalized(String cardNormalized) { this.cardNormalized = cardNormalized; }
    public String getCardRole() { return cardRole; }
    public void setCardRole(String cardRole) { this.cardRole = cardRole; }
    public boolean isCommanderSlot() { return commanderSlot; }
    public void setCommanderSlot(boolean commanderSlot) { this.commanderSlot = commanderSlot; }
}
