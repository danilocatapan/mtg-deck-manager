package com.mtg.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mtg.domain.ComboAnalysis;
import com.mtg.domain.ComboHit;
import com.mtg.domain.ComboNearMiss;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class ComboDetectionService {

    private static final Logger LOG = Logger.getLogger(ComboDetectionService.class);
    private static final String RESOURCE = "analysis/known-combos.json";
    private static final String SOURCE = "Commander Spellbook / EDH-Combos local snapshot";

    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final AtomicReference<List<ComboDefinition>> combos = new AtomicReference<>();

    public ComboAnalysis analyze(Set<String> deckCardNames) {
        if (deckCardNames == null || deckCardNames.isEmpty()) {
            return ComboAnalysis.empty();
        }

        Set<String> normalizedDeck = new HashSet<>();
        deckCardNames.stream().map(this::normalize).forEach(normalizedDeck::add);

        List<ComboHit> present = new java.util.ArrayList<>();
        List<ComboNearMiss> oneCardAway = new java.util.ArrayList<>();

        ComboSnapshot snapshot = load();
        for (ComboDefinition combo : snapshot.combos()) {
            List<String> missing = combo.cards().stream()
                    .filter(card -> !normalizedDeck.contains(normalize(card)))
                    .toList();
            if (missing.isEmpty()) {
                present.add(new ComboHit(combo.name(), combo.cards(), combo.result(), source(combo)));
            } else if (missing.size() == 1 && combo.cards().size() <= 4) {
                List<String> presentCards = combo.cards().stream()
                        .filter(card -> normalizedDeck.contains(normalize(card)))
                        .toList();
                oneCardAway.add(new ComboNearMiss(combo.name(), presentCards, missing.getFirst(), combo.result(), source(combo)));
            }
        }

        return new ComboAnalysis(
                present.stream().limit(12).toList(),
                oneCardAway.stream().limit(12).toList(),
                snapshot.source(),
                snapshot.version(),
                snapshot.updatedAt()
        );
    }

    public List<ComboCompletionSignal> completionSignals(Set<String> deckCardNames) {
        ComboAnalysis analysis = analyze(deckCardNames);
        return analysis.oneCardAway().stream()
                .map(nearMiss -> new ComboCompletionSignal(nearMiss.missingCard(), nearMiss.name()))
                .toList();
    }

    public Set<String> protectedPieces(Set<String> deckCardNames) {
        ComboAnalysis analysis = analyze(deckCardNames);
        Set<String> protectedNames = new HashSet<>();
        for (ComboHit hit : analysis.present()) {
            hit.cards().stream().map(this::normalize).forEach(protectedNames::add);
        }
        for (ComboNearMiss nearMiss : analysis.oneCardAway()) {
            nearMiss.presentCards().stream().map(this::normalize).forEach(protectedNames::add);
        }
        return protectedNames;
    }

    private ComboSnapshot load() {
        List<ComboDefinition> current = combos.get();
        if (current != null) {
            return readMetadata(current);
        }
        ComboSnapshot loaded = readSnapshot();
        combos.compareAndSet(null, loaded.combos());
        return loaded;
    }

    private ComboSnapshot readSnapshot() {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                LOG.warnv("event=combo.snapshot.missing resource={0}", RESOURCE);
                return ComboSnapshot.empty();
            }
            ComboSnapshot snapshot = mapper.readValue(stream, ComboSnapshot.class);
            LOG.infov("event=combo.snapshot.loaded source={0} combos={1}", snapshot.source(), snapshot.combos().size());
            return snapshot;
        } catch (Exception exception) {
            LOG.warnv(exception, "event=combo.snapshot.load_failed resource={0}", RESOURCE);
            return ComboSnapshot.empty();
        }
    }

    private ComboSnapshot readMetadata(List<ComboDefinition> currentCombos) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                return new ComboSnapshot(SOURCE, "unknown", "unknown", currentCombos);
            }
            ComboSnapshot snapshot = mapper.readValue(stream, ComboSnapshot.class);
            return new ComboSnapshot(snapshot.source(), snapshot.version(), snapshot.updatedAt(), currentCombos);
        } catch (Exception exception) {
            return new ComboSnapshot(SOURCE, "unknown", "unknown", currentCombos);
        }
    }

    private String source(ComboDefinition combo) {
        return combo.source() == null || combo.source().isBlank() ? SOURCE : combo.source();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ComboSnapshot(String source, String version, String updatedAt, List<ComboDefinition> combos) {
        ComboSnapshot {
            source = source == null || source.isBlank() ? SOURCE : source;
            version = version == null || version.isBlank() ? "unknown" : version;
            updatedAt = updatedAt == null || updatedAt.isBlank() ? "unknown" : updatedAt;
            combos = combos == null ? List.of() : List.copyOf(combos);
        }

        static ComboSnapshot empty() {
            return new ComboSnapshot(SOURCE, "unknown", "unknown", List.of());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ComboDefinition(String name, List<String> cards, String result, String source) {
        ComboDefinition {
            cards = cards == null ? List.of() : List.copyOf(cards);
        }
    }

    public record ComboCompletionSignal(String missingCard, String comboName) {
    }
}
