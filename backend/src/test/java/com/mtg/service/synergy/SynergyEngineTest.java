package com.mtg.service.synergy;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SynergyEngineTest {

    @Test
    public void computesSynergyCorrectly() {
        SynergyEngine engine = new SynergyEngine();
        Set<String> cardTags = Set.of("draw", "haste", "combat");
        Set<String> deckTags = Set.of("draw", "ramp", "combat", "token");
        Set<String> commanderTags = Set.of("combat", "big-creature");

        double synergy = engine.computeSynergy(cardTags, deckTags, commanderTags);
        // pool = draw,ramp,combat,token,big-creature => size 5
        // intersection = draw,combat => size 2 => 2/5 = 0.4
        assertEquals(0.4, synergy, 0.0001);
    }
}
