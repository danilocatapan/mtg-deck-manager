package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class MetaProviderImpl implements MetaProvider {

    @Inject
    MetaDatasetLoader loader;

    @Override
    public List<MetaCard> getTopCards(String commander) {
        if (commander == null) return List.of();
        return loader.getCardsForCommander(commander);
    }
}
