package com.mtg.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeckImportServiceTest {

    private final DeckImportService service = new DeckImportService();

    @Test
    void parsesMoxfieldPrintingAndFoilMetadata() {
        var cards = service.parse("""
                1 Winota, Joiner of Forces (IKO) 349 *F*
                1 Ainok Strike Leader (TDC) 51
                """);

        assertEquals(2, cards.size());
        assertEquals("Winota, Joiner of Forces", cards.get(0).getName());
        assertEquals("IKO", cards.get(0).getSetCode());
        assertEquals("349", cards.get(0).getCollectorNumber());
        assertEquals("FOIL", cards.get(0).getFinish());
        assertEquals("Ainok Strike Leader", cards.get(1).getName());
        assertEquals("TDC", cards.get(1).getSetCode());
        assertEquals("51", cards.get(1).getCollectorNumber());
        assertEquals("UNKNOWN", cards.get(1).getFinish());
    }

    @Test
    void parsesArchidektSetOnlyAndLigamagicPlainText() {
        var cards = service.parse("""
                1x Sol Ring (LTC)
                2 Forest
                """);

        assertEquals("Sol Ring", cards.get(0).getName());
        assertEquals("LTC", cards.get(0).getSetCode());
        assertNull(cards.get(0).getCollectorNumber());
        assertEquals("Forest", cards.get(1).getName());
        assertEquals(2, cards.get(1).getQuantity());
    }

    @Test
    void ignoresArenaSideboardHeaders() {
        var cards = service.parse("""
                Deck
                1 Command Tower (CMM) 425

                Sideboard
                1 Pyroblast
                """);

        assertEquals(1, cards.size());
        assertEquals("Command Tower", cards.getFirst().getName());
        assertEquals("CMM", cards.getFirst().getSetCode());
        assertEquals("425", cards.getFirst().getCollectorNumber());
    }
}
