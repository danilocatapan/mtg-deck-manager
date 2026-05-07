package com.mtg.service.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommanderBracketServiceTest {

    private final CommanderBracketService service = new CommanderBracketService();

    @Test
    void mapsLegacyAliasesToLevels() {
        assertEquals(2, service.levelFor("casual"));
        assertEquals(3, service.levelFor("mid"));
        assertEquals(4, service.levelFor("high-power"));
        assertEquals(5, service.levelFor("cedh"));
    }

    @Test
    void keepsNumericModelAvailable() {
        assertEquals("precon", service.normalizeAlias("1"));
        assertEquals("casual", service.normalizeAlias("2"));
        assertEquals("mid", service.normalizeAlias("3"));
        assertEquals("high-power", service.normalizeAlias("4"));
        assertEquals("cedh", service.normalizeAlias("5"));
    }
}
