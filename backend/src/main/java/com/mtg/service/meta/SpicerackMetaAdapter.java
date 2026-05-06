package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SpicerackMetaAdapter extends AbstractCachedMetaSourceAdapter {
    @Override
    public String sourceName() {
        return "Spicerack";
    }

    @Override
    public List<String> supportedBrackets() {
        return List.of("mid", "high-power", "cedh");
    }
}
