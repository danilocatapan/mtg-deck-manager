package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CardCollectionRequestDTO {
    private List<String> names = new ArrayList<>();

    public CardCollectionRequestDTO() {
    }

    public CardCollectionRequestDTO(List<String> names) {
        this.names = names == null ? new ArrayList<>() : new ArrayList<>(names);
    }

    public List<String> names() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names == null ? new ArrayList<>() : new ArrayList<>(names);
    }
}
