package com.mtg.service.meta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DecklistNormalizerTest {

    private final DecklistNormalizer normalizer = new DecklistNormalizer();

    @Test
    void parsesQuantityAndCardName() {
        var cards = normalizer.normalizePlainText("""
                1 Sol Ring
                10 Forest
                """);

        assertEquals(2, cards.size());
        assertEquals("Sol Ring", cards.get(0).name());
        assertEquals(1, cards.get(0).quantity());
        assertEquals("Forest", cards.get(1).name());
        assertEquals(10, cards.get(1).quantity());
    }

    @Test
    void ignoresEmptyCommentAndInvalidLines() {
        var cards = normalizer.normalizePlainText("""
                Commander
                // comment

                this is not a card line
                1 Arcane Signet
                """);

        assertEquals(1, cards.size());
        assertEquals("Arcane Signet", cards.getFirst().name());
    }

    @Test
    void removesSetInformation() {
        var cards = normalizer.normalizePlainText("1 Sol Ring (LTC) [foil]");

        assertEquals(1, cards.size());
        assertEquals("Sol Ring", cards.getFirst().name());
    }

    @Test
    void findsCommanderFromCommanderSection() {
        String commander = normalizer.findCommander("""
                Commander
                1 Atraxa, Praetors' Voice

                Deck
                1 Sol Ring
                """);

        assertEquals("Atraxa, Praetors' Voice", commander);
    }

    @Test
    void returnsNullWhenCommanderIsMissing() {
        assertNull(normalizer.findCommander("1 Sol Ring"));
    }
}
