package com.mtg.service.meta;

import com.mtg.repository.MetaDeckSnapshotRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class MetaDatasetServiceTest {

    @Inject
    MetaDatasetService service;

    @Inject
    MetaDeckSnapshotRepository repository;

    @BeforeEach
    void clean() {
        QuarkusTransaction.requiringNew().run(repository::deleteSnapshots);
    }

    @Test
    void persistsAndReloadsCanonicalDecks() {
        service.replaceBySource("TopDeck", List.of(deck("TopDeck", "1")));

        assertEquals(1, service.findAll().size());
        assertEquals("Xenagos, God of Revels", service.findAll().getFirst().commander());
    }

    @Test
    void replacesOnlyDecksFromSameSource() {
        service.replaceBySource("TopDeck", List.of(deck("TopDeck", "1")));
        service.replaceBySource("LOCAL_TEST", List.of(deck("LOCAL_TEST", "2")));
        service.replaceBySource("TopDeck", List.of(deck("TopDeck", "3")));

        assertEquals(2, service.findAll().size());
        assertEquals(1, service.findAll().stream().filter(deck -> deck.source().equals("TopDeck")).count());
        assertEquals("3", service.findAll().stream().filter(deck -> deck.source().equals("TopDeck")).findFirst().orElseThrow().externalId());
    }

    private MetaDeck deck(String source, String id) {
        return new MetaDeck(
                source,
                id,
                "Xenagos, God of Revels",
                List.of(),
                List.of("R", "G"),
                "high-power",
                List.of(),
                List.of(new MetaDeckCard("Sol Ring", 1, List.of(), null, null, List.of())),
                null,
                null,
                1,
                64,
                null,
                OffsetDateTime.now()
        );
    }
}
