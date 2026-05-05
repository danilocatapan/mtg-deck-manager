package com.mtg.service.meta;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class MetaDatasetLoader {
    private static final Logger LOG = Logger.getLogger(MetaDatasetLoader.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, MetaCommander> dataset = new ConcurrentHashMap<>();

    @PostConstruct
    public void load() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("meta_dataset.json")) {
            if (is == null) {
                LOG.warn("meta_dataset.json not found on classpath");
                return;
            }
            List<MetaCommander> list = mapper.readValue(is, new TypeReference<>() {});
            for (MetaCommander mc : list) {
                if (mc.getCommander() != null) dataset.put(mc.getCommander(), mc);
            }
            LOG.infov("meta.dataset.loaded commanders={0}", dataset.size());
        } catch (Exception e) {
            LOG.error("failed loading meta_dataset.json", e);
        }
    }

    public List<MetaCard> getCardsForCommander(String commander) {
        MetaCommander mc = dataset.get(commander);
        if (mc == null) return Collections.emptyList();
        return mc.getCards();
    }

    public Map<String, MetaCommander> getDatasetMap() { return dataset; }
}
