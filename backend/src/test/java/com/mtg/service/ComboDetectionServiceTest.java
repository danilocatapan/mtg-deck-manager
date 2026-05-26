package com.mtg.service;

import com.mtg.model.MetaCombo;
import com.mtg.model.MetaComboCard;
import com.mtg.repository.MetaComboRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
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

    @Test
    void usesPersistedCombosAndCommanderNamesWhenAvailable() {
        ComboDetectionService persistedService = new ComboDetectionService();
        MetaComboRepository repository = Mockito.mock(MetaComboRepository.class);
        persistedService.comboRepository = repository;

        MetaCombo combo = combo("K'rrik + Vilis + Aetherflux Reservoir",
                "K'rrik, Son of Yawgmoth",
                "Vilis, Broker of Blood",
                "Aetherflux Reservoir");
        Mockito.when(repository.listUsableCombos()).thenReturn(List.of(combo));

        Set<String> deckNames = Set.of("K'rrik, Son of Yawgmoth", "Vilis, Broker of Blood", "Swamp");
        var signals = persistedService.completionSignals(deckNames);
        var protectedPieces = persistedService.protectedPieces(deckNames);

        assertEquals("Aetherflux Reservoir", signals.getFirst().missingCard());
        assertTrue(protectedPieces.contains("k'rrik, son of yawgmoth"));
        assertTrue(protectedPieces.contains("vilis, broker of blood"));
    }

    private MetaCombo combo(String name, String... cards) {
        MetaCombo combo = new MetaCombo();
        combo.setSource("Commander Spellbook");
        combo.setExternalId(name.toLowerCase());
        combo.setName(name);
        combo.setResultText("combo test");
        combo.setSyncedAt(OffsetDateTime.now());
        combo.setCards(java.util.Arrays.stream(cards)
                .map(this::comboCard)
                .toList());
        return combo;
    }

    private MetaComboCard comboCard(String name) {
        MetaComboCard card = new MetaComboCard();
        card.setCardName(name);
        card.setCardNormalized(name.toLowerCase());
        return card;
    }
}
