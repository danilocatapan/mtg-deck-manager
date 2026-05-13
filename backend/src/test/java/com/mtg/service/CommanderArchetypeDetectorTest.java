package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.service.synergy.SynergyEngine;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class CommanderArchetypeDetectorTest {

    @Test
    void detectsSimpleArchetypesFromTagsAndRoleSummary() {
        CommanderArchetypeDetector detector = detectorWithCommanderTags(Set.of());

        assertEquals("combat", detector.detect("Xenagos", null, summary(Set.of("combat", "trample"), 4, 4.0), Set.of("R", "G")).archetype());
        assertEquals("control", detector.detect("Talion", null, summary(Set.of("counterspell", "removal"), 12, 2.8), Set.of("U", "B")).archetype());
        assertEquals("turbo-combo", detector.detect("Rograkh/Silas", null, summary(Set.of("combo-piece", "tutor"), 8, 2.1), Set.of("U", "B", "R")).archetype());
    }

    private CommanderArchetypeDetector detectorWithCommanderTags(Set<String> tags) {
        SynergyEngine synergyEngine = Mockito.mock(SynergyEngine.class);
        when(synergyEngine.tagsForCard(Mockito.any())).thenReturn(tags);
        CommanderArchetypeDetector detector = new CommanderArchetypeDetector();
        detector.synergyEngine = synergyEngine;
        return detector;
    }

    private DeckRoleSummary summary(Set<String> tags, int removal, double averageCmc) {
        return new DeckRoleSummary(99, 30, 10, 10, removal, 4, 1, 3, averageCmc, Map.of(), tags);
    }
}
