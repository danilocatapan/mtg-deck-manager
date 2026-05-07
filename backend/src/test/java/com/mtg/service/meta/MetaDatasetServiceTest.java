package com.mtg.service.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetaDatasetServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsDecksFromJsonFile() {
        Path file = tempDir.resolve("meta-decks.json");

        MetaDatasetService writer = new MetaDatasetService();
        writer.datasetFile = file;
        writer.load();
        writer.replaceBySource("Spicerack", List.of(deck("Spicerack", "1")));

        MetaDatasetService reader = new MetaDatasetService();
        reader.datasetFile = file;
        reader.load();

        assertEquals(1, reader.findAll().size());
        assertEquals("Xenagos, God of Revels", reader.findAll().getFirst().commander());
    }

    @Test
    void replacesOnlyDecksFromSameSource() {
        MetaDatasetService service = new MetaDatasetService();
        service.datasetFile = tempDir.resolve("meta-decks.json");
        service.load();

        service.replaceBySource("Spicerack", List.of(deck("Spicerack", "1")));
        service.replaceBySource("TopDeck.gg", List.of(deck("TopDeck.gg", "2")));
        service.replaceBySource("Spicerack", List.of(deck("Spicerack", "3")));

        assertEquals(2, service.findAll().size());
        assertEquals(1, service.findAll().stream().filter(deck -> deck.source().equals("Spicerack")).count());
    }

    private MetaDeck deck(String source, String id) {
        return new MetaDeck(
                source,
                id,
                "Xenagos, God of Revels",
                List.of(),
                List.of(),
                "mid",
                List.of(),
                List.of(new MetaDeckCard("Sol Ring", 1, List.of(), null, null, List.of())),
                null,
                null,
                null,
                null,
                null,
                OffsetDateTime.now()
        );
    }
}
