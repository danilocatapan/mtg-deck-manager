package com.mtg.service.meta;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class MetaDatasetService {
    private static final Logger LOG = Logger.getLogger(MetaDatasetService.class);

    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
    private final List<MetaDeck> decks = new CopyOnWriteArrayList<>();

    @ConfigProperty(name = "meta.dataset.file", defaultValue = "data/meta-decks.json")
    Path datasetFile;

    @PostConstruct
    public void load() {
        decks.clear();
        if (!Files.exists(datasetFile)) {
            LOG.infov("event=meta.dataset.loaded decks=0 file={0}", datasetFile);
            return;
        }

        try {
            decks.addAll(mapper.readValue(datasetFile.toFile(), new TypeReference<List<MetaDeck>>() {}));
            LOG.infov("event=meta.dataset.loaded decks={0} file={1}", decks.size(), datasetFile);
        } catch (Exception exception) {
            LOG.errorv(exception, "event=meta.dataset.load_failed file={0}", datasetFile);
        }
    }

    public void replaceBySource(String source, List<MetaDeck> importedDecks) {
        if (source == null || source.isBlank()) {
            return;
        }
        decks.removeIf(deck -> source.equalsIgnoreCase(deck.source()));
        if (importedDecks != null) {
            decks.addAll(importedDecks);
        }
        save();
    }

    public List<MetaDeck> findAll() {
        return new ArrayList<>(decks);
    }

    public void save() {
        try {
            Path parent = datasetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(datasetFile.toFile(), decks);
            LOG.infov("event=meta.dataset.saved decks={0} file={1}", decks.size(), datasetFile);
        } catch (Exception exception) {
            LOG.errorv(exception, "event=meta.dataset.save_failed file={0}", datasetFile);
        }
    }
}
