package com.mtg.service.meta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MetaProviderImplTest {

    @Test
    public void providerReturnsCardsForCommander() {
        MetaDatasetLoader loader = new MetaDatasetLoader();
        loader.load();
        MetaProviderImpl impl = new MetaProviderImpl();
        // inject loader manually
        impl.loader = loader;

        var cards = impl.getTopCards("Xenagos, God of Revels");
        assertNotNull(cards);
        assertFalse(cards.isEmpty());
    }
}
