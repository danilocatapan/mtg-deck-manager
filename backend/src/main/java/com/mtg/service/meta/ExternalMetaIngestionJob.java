package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;
import com.mtg.dto.MetaSyncSummaryDTO;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ExternalMetaIngestionJob {
    private static final Logger LOG = Logger.getLogger(ExternalMetaIngestionJob.class);

    @Inject
    TopDeckMetaAdapter topDeckAdapter;

    @Inject
    MetaDatasetService datasetService;

    @Inject
    CommanderMetaProfileService profileService;

    public MetaSyncSummaryDTO sync() {
        String source = topDeckAdapter.sourceName();
        LOG.infov("event=meta.sync.started source={0}", source);
        List<MetaDeck> decks = topDeckAdapter.sync();
        List<String> limitations = new java.util.ArrayList<>();
        List<String> errors = topDeckAdapter.lastSyncError() == null
                ? List.of()
                : List.of(topDeckAdapter.lastSyncError());
        boolean receivedNewSnapshot = topDeckAdapter.lastSyncSuccessful() && !decks.isEmpty();
        if (!receivedNewSnapshot) {
            limitations.add("TopDeck.gg nao retornou um snapshot novo; o ultimo snapshot valido foi preservado.");
        } else {
            datasetService.replaceBySource(source, decks);
        }
        int profilesBuilt = profileService.rebuild();
        List<MetaDeck> current = datasetService.findAll().stream()
                .filter(deck -> source.equalsIgnoreCase(deck.source()))
                .toList();
        Map<String, Integer> coverage = current.stream().collect(Collectors.groupingBy(
                deck -> deck.bracket() == null ? "unknown" : deck.bracket(),
                Collectors.summingInt(deck -> 1)
        ));
        int commanders = (int) current.stream()
                .map(MetaDeck::commander)
                .filter(value -> value != null && !value.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .count();
        String status = receivedNewSnapshot ? "success" : "preserved_last_snapshot";
        LOG.infov("event=meta.sync.completed source={0} decks={1} profiles={2}", source, current.size(), profilesBuilt);
        return new MetaSyncSummaryDTO(
                status,
                receivedNewSnapshot ? decks.size() : 0,
                topDeckAdapter.lastDiscardedDecks(),
                current.size(),
                commanders,
                coverage,
                profilesBuilt,
                errors,
                limitations,
                topDeckAdapter.status()
        );
    }

    public List<MetaDeck> cachedDecks() {
        return datasetService.findAll();
    }
}
