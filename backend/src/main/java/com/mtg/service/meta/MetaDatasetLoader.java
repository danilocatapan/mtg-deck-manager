package com.mtg.service.meta;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class MetaDatasetLoader {
    private static final Logger LOG = Logger.getLogger(MetaDatasetLoader.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, MetaCommander> dataset = new ConcurrentHashMap<>();

    @PostConstruct
    public void load() {
        dataset.clear();
        boolean loaded = loadFromCommandersFolder();
        if (!loaded) {
            loadFromLegacyDataset();
        }
        LOG.infov("meta.dataset.loaded commanders={0}", dataset.size());
    }

    private boolean loadFromCommandersFolder() {
        try {
            List<URL> resources = Collections.list(
                    getClass().getClassLoader().getResources("meta/commanders")
            );

            if (resources.isEmpty()) {
                return false;
            }

            // In packaged apps listing resources can be tricky; use an index file.
            try (InputStream indexIs = getClass().getClassLoader().getResourceAsStream("meta/commanders/index.json")) {
                if (indexIs == null) {
                    LOG.warn("meta/commanders/index.json not found; using legacy meta_dataset.json");
                    return false;
                }
                List<String> files = mapper.readValue(indexIs, new TypeReference<>() {});
                for (String file : files) {
                    try (InputStream commanderIs = getClass().getClassLoader().getResourceAsStream("meta/commanders/" + file)) {
                        if (commanderIs == null) continue;
                        MetaCommander mc = mapper.readValue(commanderIs, MetaCommander.class);
                        addCommander(mc);
                    }
                }
                return !dataset.isEmpty();
            }
        } catch (Exception e) {
            LOG.error("failed loading meta/commanders dataset", e);
            return false;
        }
    }

    private void loadFromLegacyDataset() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("meta_dataset.json")) {
            if (is == null) {
                LOG.warn("meta_dataset.json not found on classpath");
                return;
            }
            List<MetaCommander> list = mapper.readValue(is, new TypeReference<>() {});
            for (MetaCommander mc : list) {
                addCommander(mc);
            }
        } catch (Exception e) {
            LOG.error("failed loading meta_dataset.json", e);
        }
    }

    private void addCommander(MetaCommander mc) {
        if (mc == null || mc.getCommander() == null || mc.getCommander().isBlank()) return;
        dataset.put(normalizeCommanderName(mc.getCommander()), mc);
    }

    private String normalizeCommanderName(String commander) {
        String normalized = Objects.requireNonNullElse(commander, "").trim();
        if (normalized.startsWith("A-")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    public List<MetaCard> getCardsForCommander(String commander) {
        MetaCommander mc = dataset.get(normalizeCommanderName(commander));
        if (mc == null) return Collections.emptyList();
        return mc.getCards();
    }

    public Map<String, MetaCommander> getDatasetMap() { return dataset; }
}
