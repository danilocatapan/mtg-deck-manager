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

    public List<MetaSourceStatus> sync() {
        List<MetaSourceStatus> statuses = new ArrayList<>();
        if (adapters == null) {
            return statuses;
        }
        for (MetaSourceAdapter adapter : adapters) {
            LOG.infov("event=meta.sync.source source={0} mode=offline-cache", adapter.sourceName());
            statuses.add(adapter.status());
        }
        return statuses;
    }
}
