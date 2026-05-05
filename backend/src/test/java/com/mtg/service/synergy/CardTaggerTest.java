package com.mtg.service.synergy;

import com.mtg.dto.CardResponseDTO;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CardTaggerTest {

    @Test
    public void tagsExtractedFromOracleTextAndType() {
        CardTagger tagger = new CardTagger();
        CardResponseDTO card = new CardResponseDTO(
            "Test Card",
            "{1}{G}",
            "Creature — Elf",
            "When Test Card enters the battlefield, draw a card. It has haste and trample.",
            3.0,
            java.util.List.of("G"),
            java.util.List.of("haste","trample")
        );

        Set<String> tags = tagger.tagCard(card);
        assertTrue(tags.contains("draw"));
        assertTrue(tags.contains("haste"));
        assertTrue(tags.contains("trample"));
    }
}
