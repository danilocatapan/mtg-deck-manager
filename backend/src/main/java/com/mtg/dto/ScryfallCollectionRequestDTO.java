package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ScryfallCollectionRequestDTO {
    private List<CardIdentifier> identifiers = new ArrayList<>();

    public ScryfallCollectionRequestDTO() {
    }

    public ScryfallCollectionRequestDTO(List<CardIdentifier> identifiers) {
        this.identifiers = identifiers == null ? new ArrayList<>() : new ArrayList<>(identifiers);
    }

    public List<CardIdentifier> identifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<CardIdentifier> identifiers) {
        this.identifiers = identifiers == null ? new ArrayList<>() : new ArrayList<>(identifiers);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ScryfallCollectionRequestDTO that)) {
            return false;
        }
        return Objects.equals(identifiers, that.identifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifiers);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class CardIdentifier {
        private String name;
        @JsonProperty("set")
        private String setCode;
        @JsonProperty("collector_number")
        private String collectorNumber;
        private String id;

        public CardIdentifier() {
        }

        public CardIdentifier(String name) {
            this(name, null, null, null);
        }

        public CardIdentifier(String name, String setCode, String collectorNumber, String id) {
            this.name = name;
            this.setCode = setCode;
            this.collectorNumber = collectorNumber;
            this.id = id;
        }

        public static CardIdentifier printing(String setCode, String collectorNumber) {
            return new CardIdentifier(null, setCode, collectorNumber, null);
        }

        public String name() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String setCode() {
            return setCode;
        }

        public void setSetCode(String setCode) {
            this.setCode = setCode;
        }

        public String collectorNumber() {
            return collectorNumber;
        }

        public void setCollectorNumber(String collectorNumber) {
            this.collectorNumber = collectorNumber;
        }

        public String id() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CardIdentifier that)) {
                return false;
            }
            return Objects.equals(name, that.name)
                    && Objects.equals(setCode, that.setCode)
                    && Objects.equals(collectorNumber, that.collectorNumber)
                    && Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, setCode, collectorNumber, id);
        }
    }
}
