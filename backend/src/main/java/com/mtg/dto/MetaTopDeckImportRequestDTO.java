package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.time.LocalDate;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MetaTopDeckImportRequestDTO {
    private String source;
    private String sourceUrl;
    private String rankingPeriod;
    private LocalDate rankingDate;
    private String format;
    private String importFormat;
    private String decklistFormat;
    private List<MetaTopDeckImportDeckDTO> decks;

    public MetaTopDeckImportRequestDTO() {
    }

    public MetaTopDeckImportRequestDTO(
            String source,
            String sourceUrl,
            String rankingPeriod,
            LocalDate rankingDate,
            String format,
            String importFormat,
            String decklistFormat,
            List<MetaTopDeckImportDeckDTO> decks
    ) {
        this.source = source;
        this.sourceUrl = sourceUrl;
        this.rankingPeriod = rankingPeriod;
        this.rankingDate = rankingDate;
        this.format = format;
        this.importFormat = importFormat;
        this.decklistFormat = decklistFormat;
        this.decks = decks;
    }

    public String source() { return source; }
    public void setSource(String source) { this.source = source; }
    public String sourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String rankingPeriod() { return rankingPeriod; }
    public void setRankingPeriod(String rankingPeriod) { this.rankingPeriod = rankingPeriod; }
    public LocalDate rankingDate() { return rankingDate; }
    public void setRankingDate(LocalDate rankingDate) { this.rankingDate = rankingDate; }
    public String format() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String importFormat() { return importFormat; }
    public void setImportFormat(String importFormat) { this.importFormat = importFormat; }
    public String decklistFormat() { return decklistFormat; }
    public void setDecklistFormat(String decklistFormat) { this.decklistFormat = decklistFormat; }
    public List<MetaTopDeckImportDeckDTO> decks() { return decks; }
    public void setDecks(List<MetaTopDeckImportDeckDTO> decks) { this.decks = decks; }
}
