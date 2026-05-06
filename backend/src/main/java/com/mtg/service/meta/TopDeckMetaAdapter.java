package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class TopDeckMetaAdapter extends AbstractCachedMetaSourceAdapter {
    @Override
    public String sourceName() {
        return "TopDeck.gg";
    }

    @Override
    public List<String> supportedBrackets() {
        return List.of("high-power", "cedh");
    }
}
