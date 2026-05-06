package com.mtg.service;

import com.mtg.model.DeckCard;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class DeckImportService {

    private static final int MAX_IMPORT_LINES = 120;
    private static final int MAX_LINE_LENGTH = 160;

    public List<DeckCard> parse(String content) {
        if (content == null || content.isBlank()) return List.of();

        String[] lines = content.split("\n");
        if (lines.length > MAX_IMPORT_LINES) {
            throw new IllegalArgumentException("Deck import accepts at most " + MAX_IMPORT_LINES + " lines");
        }

        return Arrays.stream(lines)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .peek(this::validateLineLength)
                .map(this::parseLine)
                .toList();
    }

    private void validateLineLength(String line) {
        if (line.length() > MAX_LINE_LENGTH) {
            throw new IllegalArgumentException("Deck import line is too long");
        }
    }

    private DeckCard parseLine(String line) {
        try {
            String[] parts = line.split(" ", 2);
            int quantity = Integer.parseInt(parts[0]);
            String name = parts[1].trim();
            if (quantity < 1 || quantity > 99) {
                throw new IllegalArgumentException("Invalid card quantity");
            }
            if (name.isBlank()) {
                throw new IllegalArgumentException("Card name is required");
            }
            return new DeckCard(name, quantity);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid deck import line");
        }
    }
}
