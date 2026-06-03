package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ExternalDeckImportRequestDTO {
    private String source;
    private String sourceUrl;
    private String format;
    private String importFormat;
    private String decklistFormat;
    private List<ExternalDeckImportDeckDTO> decks;

    public ExternalDeckImportRequestDTO() {
    }

    public ExternalDeckImportRequestDTO(
            String source,
            String sourceUrl,
            String format,
            String importFormat,
            String decklistFormat,
            List<ExternalDeckImportDeckDTO> decks
    ) {
        this.source = source;
        this.sourceUrl = sourceUrl;
        this.format = format;
        this.importFormat = importFormat;
        this.decklistFormat = decklistFormat;
        this.decks = decks;
    }

    public String source() { return source; }
    public void setSource(String source) { this.source = source; }
    public String sourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String format() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String importFormat() { return importFormat; }
    public void setImportFormat(String importFormat) { this.importFormat = importFormat; }
    public String decklistFormat() { return decklistFormat; }
    public void setDecklistFormat(String decklistFormat) { this.decklistFormat = decklistFormat; }
    public List<ExternalDeckImportDeckDTO> decks() { return decks; }
    public void setDecks(List<ExternalDeckImportDeckDTO> decks) { this.decks = decks; }
}
