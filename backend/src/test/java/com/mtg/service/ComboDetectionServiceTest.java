package com.mtg.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ComboDetectionServiceTest {

    ComboDetectionService service = new ComboDetectionService();

    @Test
    void identifiesPresentAndOneCardAwayCombos() {
        var analysis = service.analyze(Set.of("Thassa's Oracle", "Demonic Consultation", "Underworld Breach", "Lion's Eye Diamond"));

        assertTrue(analysis.present().stream().anyMatch(combo -> combo.name().equals("Thassa's Oracle + Demonic Consultation")));
        assertTrue(analysis.oneCardAway().stream().anyMatch(combo -> combo.missingCard().equals("Brain Freeze")));
    }

    @Test
    void exposesCompletionSignalsAndProtectedPiecesForRecommendations() {
        var signals = service.completionSignals(Set.of("Underworld Breach", "Lion's Eye Diamond"));
        var protectedPieces = service.protectedPieces(Set.of("Underworld Breach", "Lion's Eye Diamond"));

        assertTrue(signals.stream().anyMatch(signal -> signal.missingCard().equals("Brain Freeze")));
        assertTrue(protectedPieces.contains("underworld breach"));
        assertTrue(protectedPieces.contains("lion's eye diamond"));
    }
}
