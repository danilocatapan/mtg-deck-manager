package com.mtg.service.meta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MetaDatasetLoaderTest {

    @Test
    public void loadsDatasetFromResources() {
        MetaDatasetLoader loader = new MetaDatasetLoader();
        // call load directly to simulate @PostConstruct
        loader.load();
        assertNotNull(loader.getDatasetMap());
        // sample dataset contains Xenagos
        assertTrue(loader.getDatasetMap().containsKey("Xenagos, God of Revels"));
        MetaCommander mc = loader.getDatasetMap().get("Xenagos, God of Revels");
        assertNotNull(mc.getCards());
        assertFalse(mc.getCards().isEmpty());
    }
}
