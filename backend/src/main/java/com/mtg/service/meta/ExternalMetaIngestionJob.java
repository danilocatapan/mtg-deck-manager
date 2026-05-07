package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ExternalMetaIngestionJob {
    private static final Logger LOG = Logger.getLogger(ExternalMetaIngestionJob.class);

    @Inject
    Instance<MetaSourceAdapter> adapters;

    @Inject
    MetaDatasetService datasetService;

    public List<MetaSourceStatus> sync() {
        List<MetaSourceStatus> statuses = new ArrayList<>();
        if (adapters == null) {
            return statuses;
        }
        for (MetaSourceAdapter adapter : adapters) {
            String source = adapter.sourceName();
            LOG.infov("event=meta.sync.started source={0}", source);
            try {
                List<MetaDeck> decks = adapter.sync();
                if (!decks.isEmpty()) {
                    datasetService.replaceBySource(source, decks);
                }
                LOG.infov("event=meta.sync.completed source={0} decks={1}", source, decks.size());
            } catch (Exception exception) {
                LOG.errorv(exception, "event=meta.sync.failed source={0}", source);
            } finally {
                statuses.add(adapter.status());
            }
        }
        return statuses;
    }

    public List<MetaDeck> cachedDecks() {
        return datasetService.findAll();
    }
}
