package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class ArchetypeClassifier {
    public List<String> classify(MetaDeck deck) {
        String text = deck == null || deck.cards() == null ? "" : deck.cards().stream()
                .map(MetaDeckCard::name)
                .reduce("", (left, right) -> left + " " + right)
                .toLowerCase(Locale.ROOT);
        if (text.contains("food chain") || text.contains("thoracle") || text.contains("thassa's oracle")) {
            return List.of("combo");
        }
        if (text.contains("counterspell") || text.contains("force of will")) {
            return List.of("control");
        }
        return List.of("value");
    }
}
