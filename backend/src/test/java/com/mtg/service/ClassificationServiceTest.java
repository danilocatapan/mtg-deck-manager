package com.mtg.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassificationServiceTest {

    private final ClassificationService svc = new ClassificationService();

    @Test
    void detectsRamp() {
        assertEquals(ClassificationService.CardCategory.RAMP, svc.classify("{T}: Add {G} to your mana pool."));
    }

    @Test
    void detectsDraw() {
        assertEquals(ClassificationService.CardCategory.DRAW, svc.classify("Draw two cards."));
    }

    @Test
    void detectsRemoval() {
        assertEquals(ClassificationService.CardCategory.REMOVAL, svc.classify("Destroy target creature."));
        assertEquals(ClassificationService.CardCategory.REMOVAL, svc.classify("Exile target permanent."));
    }
}
