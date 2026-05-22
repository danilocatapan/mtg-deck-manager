package com.mtg.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ScryfallCollectionRequestDTO(
        List<CardIdentifier> identifiers
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CardIdentifier(
            String name,
            @JsonProperty("set") String setCode,
            @JsonProperty("collector_number") String collectorNumber,
            String id
    ) {
        public CardIdentifier(String name) {
            this(name, null, null, null);
        }

        public static CardIdentifier printing(String setCode, String collectorNumber) {
            return new CardIdentifier(null, setCode, collectorNumber, null);
        }
    }
}
