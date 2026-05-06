package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class EdhTop16MetaAdapter extends AbstractCachedMetaSourceAdapter {
    @Override
    public String sourceName() {
        return "EDHTop16";
    }

    @Override
    public List<String> supportedBrackets() {
        return List.of("cedh");
    }
}
