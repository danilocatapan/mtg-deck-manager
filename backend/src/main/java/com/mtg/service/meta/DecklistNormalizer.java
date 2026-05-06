package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class DecklistNormalizer {
    private static final Pattern LINE = Pattern.compile("^\\s*(\\d+)\\s+(.+?)\\s*$");

    public List<MetaDeckCard> normalizePlainText(String decklist) {
        if (decklist == null || decklist.isBlank()) {
            return List.of();
        }
        List<MetaDeckCard> cards = new ArrayList<>();
        for (String rawLine : decklist.split("\\R")) {
            Matcher matcher = LINE.matcher(rawLine);
            if (!matcher.matches()) {
                continue;
            }
            int quantity = Integer.parseInt(matcher.group(1));
            String name = matcher.group(2).replaceAll("\\s+\\(.+\\)$", "").trim();
            if (!name.isBlank()) {
                cards.add(new MetaDeckCard(name, quantity, List.of(), null, null, List.of()));
            }
        }
        return cards;
    }
}
