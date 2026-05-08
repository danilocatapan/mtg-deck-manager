package com.mtg.service.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommanderGameChangerServiceTest {

    private final CommanderGameChangerService service = new CommanderGameChangerService();

    @Test
    void loadsVersionedSnapshotAndDetectsCards() {
        CommanderGameChangerService.GameChangerSnapshot snapshot = service.load();

        assertNotNull(snapshot.effectiveDate());
        assertNotNull(snapshot.bracketVersion());
        assertTrue(snapshot.normalizedCards().size() > 40);
        assertTrue(service.isGameChanger("Rhystic Study"));
        assertTrue(service.isGameChanger("  rhystic study "));
        assertFalse(service.isGameChanger("Colossal Dreadmaw"));
    }
}
