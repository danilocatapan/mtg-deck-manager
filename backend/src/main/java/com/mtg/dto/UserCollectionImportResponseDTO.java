package com.mtg.dto;

import java.util.List;

public class UserCollectionImportResponseDTO {
    private int importedCards;
    private int uniqueCards;
    private boolean replacedExisting;
    private List<String> warnings;

    public UserCollectionImportResponseDTO() {
    }

    public UserCollectionImportResponseDTO(int importedCards, int uniqueCards, boolean replacedExisting, List<String> warnings) {
        this.importedCards = importedCards;
        this.uniqueCards = uniqueCards;
        this.replacedExisting = replacedExisting;
        this.warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public int getImportedCards() { return importedCards; }

    public void setImportedCards(int importedCards) { this.importedCards = importedCards; }

    public int getUniqueCards() { return uniqueCards; }

    public void setUniqueCards(int uniqueCards) { this.uniqueCards = uniqueCards; }

    public boolean isReplacedExisting() { return replacedExisting; }

    public void setReplacedExisting(boolean replacedExisting) { this.replacedExisting = replacedExisting; }

    public List<String> getWarnings() { return warnings == null ? List.of() : warnings; }

    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
