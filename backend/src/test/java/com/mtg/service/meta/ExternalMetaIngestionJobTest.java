package com.mtg.service.meta;

import com.mtg.dto.MetaSyncSummaryDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalMetaIngestionJobTest {

    @Test
    void persistsNewTopDeckSnapshotAndRebuildsProfiles() {
        TopDeckMetaAdapter adapter = Mockito.mock(TopDeckMetaAdapter.class);
        MetaDatasetService dataset = Mockito.mock(MetaDatasetService.class);
        CommanderMetaProfileService profiles = Mockito.mock(CommanderMetaProfileService.class);
        MetaDeck deck = deck("one");
        when(adapter.sourceName()).thenReturn("TopDeck");
        when(adapter.sync()).thenReturn(List.of(deck));
        when(adapter.lastSyncSuccessful()).thenReturn(true);
        when(adapter.lastDiscardedDecks()).thenReturn(2);
        when(adapter.status()).thenReturn(new MetaSourceStatus("TopDeck", true, OffsetDateTime.now(), List.of("cedh"), "competitive_meta"));
        when(dataset.findAll()).thenReturn(List.of(deck));
        when(profiles.rebuild()).thenReturn(1);

        ExternalMetaIngestionJob job = new ExternalMetaIngestionJob();
        job.topDeckAdapter = adapter;
        job.datasetService = dataset;
        job.profileService = profiles;

        MetaSyncSummaryDTO result = job.sync();

        verify(dataset).replaceBySource("TopDeck", List.of(deck));
        assertEquals("success", result.status());
        assertEquals(1, result.importedDecks());
        assertEquals(2, result.discardedDecks());
        assertEquals(1, result.snapshotDecks());
        assertEquals(1, result.commandersCovered());
    }

    @Test
    void preservesLastSnapshotWhenTopDeckReturnsNothing() {
        TopDeckMetaAdapter adapter = Mockito.mock(TopDeckMetaAdapter.class);
        MetaDatasetService dataset = Mockito.mock(MetaDatasetService.class);
        CommanderMetaProfileService profiles = Mockito.mock(CommanderMetaProfileService.class);
        MetaDeck existing = deck("existing");
        when(adapter.sourceName()).thenReturn("TopDeck");
        when(adapter.sync()).thenReturn(List.of());
        when(adapter.lastSyncSuccessful()).thenReturn(false);
        when(adapter.lastSyncError()).thenReturn("topdeck_http_429");
        when(adapter.status()).thenReturn(new MetaSourceStatus("TopDeck", true, null, List.of("cedh"), "competitive_meta"));
        when(dataset.findAll()).thenReturn(List.of(existing));

        ExternalMetaIngestionJob job = new ExternalMetaIngestionJob();
        job.topDeckAdapter = adapter;
        job.datasetService = dataset;
        job.profileService = profiles;

        MetaSyncSummaryDTO result = job.sync();

        verify(dataset, never()).replaceBySource(Mockito.anyString(), Mockito.anyList());
        assertEquals("preserved_last_snapshot", result.status());
        assertEquals(0, result.importedDecks());
        assertEquals(1, result.snapshotDecks());
        assertEquals(List.of("topdeck_http_429"), result.errors());
    }

    private MetaDeck deck(String id) {
        return new MetaDeck(
                "TopDeck",
                id,
                "Kess, Dissident Mage",
                List.of(),
                List.of("U", "B", "R"),
                "cedh",
                List.of(),
                List.of(new MetaDeckCard("Brain Freeze", 1, List.of(), null, null, List.of())),
                "Test event",
                null,
                1,
                64,
                null,
                OffsetDateTime.now()
        );
    }
}
