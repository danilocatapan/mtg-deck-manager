package com.mtg.dto;

import java.util.List;

public record ScryfallCollectionRequestDTO(
        List<CardIdentifier> identifiers
) {
    public record CardIdentifier(String name) {
    }
}
