package com.mtg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "user_card_collection_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_card_collection_owner_card",
                columnNames = {"owner_id", "card_name_normalized"}
        )
)
public class UserCardCollectionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "card_name", nullable = false)
    private String cardName;

    @Column(name = "card_name_normalized", nullable = false)
    private String cardNameNormalized;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }

    public String getOwnerId() { return ownerId; }

    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getCardName() { return cardName; }

    public void setCardName(String cardName) { this.cardName = cardName; }

    public String getCardNameNormalized() { return cardNameNormalized; }

    public void setCardNameNormalized(String cardNameNormalized) { this.cardNameNormalized = cardNameNormalized; }

    public Integer getQuantity() { return quantity; }

    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
