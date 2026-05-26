package com.mtg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtg.client.CommanderSpellbookClient;
import com.mtg.model.MetaCombo;
import com.mtg.repository.MetaComboRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommanderSpellbookComboSyncServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void importsCommanderSpellbookVariantsIntoLocalComboCache() throws Exception {
        CommanderSpellbookClient client = Mockito.mock(CommanderSpellbookClient.class);
        MetaComboRepository repository = Mockito.mock(MetaComboRepository.class);
        ComboDetectionService detector = Mockito.mock(ComboDetectionService.class);
        CommanderSpellbookComboSyncService service = new CommanderSpellbookComboSyncService();
        service.client = client;
        service.repository = repository;
        service.comboDetectionService = detector;

        JsonNode payload = mapper.readTree("""
                {
                  "results": [
                    {
                      "id": "variant-1",
                      "name": "K'rrik + Vilis + Aetherflux Reservoir",
                      "cards": [
                        {"name": "Vilis, Broker of Blood"},
                        {"name": "Aetherflux Reservoir"}
                      ],
                      "commander": "K'rrik, Son of Yawgmoth",
                      "results": ["draw and drain"],
                      "tags": ["winning"],
                      "legalities": ["commander"],
                      "popularity": 42,
                      "url": "https://commanderspellbook.com/combo/variant-1"
                    }
                  ]
                }
                """);
        Mockito.when(client.searchVariants("legal:commander", 10)).thenReturn(payload);

        int imported = service.sync("legal:commander", 10);

        ArgumentCaptor<MetaCombo> comboCaptor = ArgumentCaptor.forClass(MetaCombo.class);
        Mockito.verify(repository).persist(comboCaptor.capture());
        Mockito.verify(detector).clearCache();
        MetaCombo combo = comboCaptor.getValue();
        assertEquals(1, imported);
        assertEquals("Commander Spellbook", combo.getSource());
        assertEquals("variant-1", combo.getExternalId());
        assertEquals("winning", combo.getTags());
        assertEquals(42, combo.getPopularity());
        assertTrue(combo.getCards().stream().anyMatch(card -> card.getCardName().equals("K'rrik, Son of Yawgmoth") && card.isCommanderSlot()));
        assertTrue(combo.getCards().stream().anyMatch(card -> card.getCardName().equals("Vilis, Broker of Blood")));
    }
}
