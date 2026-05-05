package com.mtg.service;

import com.mtg.model.DeckCard;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class DeckImportService {

    public List<DeckCard> parse(String content) {
        if (content == null || content.isBlank()) return List.of();

        return Arrays.stream(content.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(this::parseLine)
                .toList();
    }

    private DeckCard parseLine(String line) {
        try {
            String[] parts = line.split(" ", 2);
            int quantity = Integer.parseInt(parts[0]);
            String name = parts[1].trim();
            return new DeckCard(name, quantity);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid line: " + line);
        }
    }
}
