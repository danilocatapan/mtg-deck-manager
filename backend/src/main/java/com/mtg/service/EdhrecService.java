package com.mtg.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class EdhrecService {

    private static final Logger LOG = Logger.getLogger(EdhrecService.class);

    private final Map<String, List<CardUsage>> cache = new ConcurrentHashMap<>();

    public static class CardUsage {
        public final String name;
        public final double inclusionRate;
        public final String category;

        public CardUsage(String name, double inclusionRate, String category) {
            this.name = name;
            this.inclusionRate = inclusionRate;
            this.category = category;
        }
    }

    public List<CardUsage> getTopCards(String commander) {
        if (commander == null || commander.isBlank()) return List.of();
        return cache.computeIfAbsent(commander, this::fetchForCommander);
    }

    private List<CardUsage> fetchForCommander(String commander) {
        // Lightweight fallback: no external calls by default.
        // This method can be extended to scrape or load precomputed datasets.
        LOG.debugv("event=edhrec.fetch fallback commander={0}", commander);
        return List.of();
    }
}
