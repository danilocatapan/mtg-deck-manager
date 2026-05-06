package com.mtg.service.meta;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class EdhrecPriorAdapter extends AbstractCachedMetaSourceAdapter {
    @Override
    public String sourceName() {
        return "EDHREC";
    }

    @Override
    public List<String> supportedBrackets() {
        return List.of("casual", "mid");
    }
}
