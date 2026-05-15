package com.mtg.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void explainsWhetherCandidateCompletesKnownCombo() {
        var standaloneOracle = service.recommendationContexts("Thassa's Oracle", Set.of("Island", "Ponder"));

        assertTrue(standaloneOracle.stream().anyMatch(context ->
                context.comboName().equals("Thassa's Oracle + Demonic Consultation")
                        && context.missingPartners().contains("Demonic Consultation")
                        && !context.completesKnownCombo()));

        var completingConsultation = service.recommendationContexts("Demonic Consultation", Set.of("Thassa's Oracle"));

        assertTrue(completingConsultation.stream().anyMatch(context ->
                context.comboName().equals("Thassa's Oracle + Demonic Consultation")
                        && context.presentPartners().contains("Thassa's Oracle")
                        && context.missingPartners().isEmpty()
                        && context.completesKnownCombo()));
        assertFalse(completingConsultation.isEmpty());
        assertEquals("Demonic Consultation", service.completionSignals(Set.of("Thassa's Oracle")).stream()
                .filter(signal -> signal.comboName().equals("Thassa's Oracle + Demonic Consultation"))
                .findFirst()
                .orElseThrow()
                .missingCard());
    }
}
