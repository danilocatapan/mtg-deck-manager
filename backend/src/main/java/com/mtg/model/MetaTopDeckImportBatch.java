package com.mtg.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "meta_top_deck_import_batches")
public class MetaTopDeckImportBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MetaDeckSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_period", nullable = false, length = 32)
    private MetaRankingPeriod rankingPeriod;

    @Column(name = "ranking_date", nullable = false)
    private LocalDate rankingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MetaImportStatus status;

    @Column(name = "imported_decks", nullable = false)
    private int importedDecks;

    @Column(name = "created_decks", nullable = false)
    private int createdDecks;

    @Column(name = "updated_decks", nullable = false)
    private int updatedDecks;

    @Column(name = "ignored_decks", nullable = false)
    private int ignoredDecks;

    @Column(name = "warnings_count", nullable = false)
    private int warningsCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MetaDeckSource getSource() { return source; }
    public void setSource(MetaDeckSource source) { this.source = source; }
    public MetaRankingPeriod getRankingPeriod() { return rankingPeriod; }
    public void setRankingPeriod(MetaRankingPeriod rankingPeriod) { this.rankingPeriod = rankingPeriod; }
    public LocalDate getRankingDate() { return rankingDate; }
    public void setRankingDate(LocalDate rankingDate) { this.rankingDate = rankingDate; }
    public MetaImportStatus getStatus() { return status; }
    public void setStatus(MetaImportStatus status) { this.status = status; }
    public int getImportedDecks() { return importedDecks; }
    public void setImportedDecks(int importedDecks) { this.importedDecks = importedDecks; }
    public int getCreatedDecks() { return createdDecks; }
    public void setCreatedDecks(int createdDecks) { this.createdDecks = createdDecks; }
    public int getUpdatedDecks() { return updatedDecks; }
    public void setUpdatedDecks(int updatedDecks) { this.updatedDecks = updatedDecks; }
    public int getIgnoredDecks() { return ignoredDecks; }
    public void setIgnoredDecks(int ignoredDecks) { this.ignoredDecks = ignoredDecks; }
    public int getWarningsCount() { return warningsCount; }
    public void setWarningsCount(int warningsCount) { this.warningsCount = warningsCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
}
