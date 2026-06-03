package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.mtg.model.DeckVisibility;
import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DeckRequestDTO {
    private String name;
    private String commander;
    private List<DeckCardDTO> cards = new ArrayList<>();
    private List<CommanderDTO> commanders;
    private DeckVisibility visibility;

    public DeckRequestDTO() {
    }

    public DeckRequestDTO(String name, String commander, List<DeckCardDTO> cards) {
        this(name, commander, cards, null, null);
    }

    public DeckRequestDTO(String name, String commander, List<DeckCardDTO> cards, List<CommanderDTO> commanders) {
        this(name, commander, cards, commanders, null);
    }

    public DeckRequestDTO(
            String name,
            String commander,
            List<DeckCardDTO> cards,
            List<CommanderDTO> commanders,
            DeckVisibility visibility
    ) {
        this.name = name;
        this.commander = commander;
        this.cards = cards == null ? new ArrayList<>() : new ArrayList<>(cards);
        this.commanders = commanders;
        this.visibility = visibility;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String commander() {
        return commander;
    }

    public void setCommander(String commander) {
        this.commander = commander;
    }

    public List<DeckCardDTO> cards() {
        return cards;
    }

    public void setCards(List<DeckCardDTO> cards) {
        this.cards = cards == null ? new ArrayList<>() : new ArrayList<>(cards);
    }

    public List<CommanderDTO> commanders() {
        return commanders;
    }

    public void setCommanders(List<CommanderDTO> commanders) {
        this.commanders = commanders;
    }

    public DeckVisibility visibility() {
        return visibility;
    }

    public void setVisibility(DeckVisibility visibility) {
        this.visibility = visibility;
    }
}
