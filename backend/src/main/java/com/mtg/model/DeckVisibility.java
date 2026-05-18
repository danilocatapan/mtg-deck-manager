package com.mtg.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum DeckVisibility {
    PRIVATE("private"),
    PUBLIC("public");

    private final String value;

    DeckVisibility(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static DeckVisibility from(String value) {
        if (value == null || value.isBlank()) {
            return PRIVATE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "private" -> PRIVATE;
            case "public" -> PUBLIC;
            default -> throw new IllegalArgumentException("Deck visibility must be public or private");
        };
    }
}
