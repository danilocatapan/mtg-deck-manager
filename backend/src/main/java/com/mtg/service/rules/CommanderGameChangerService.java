package com.mtg.service.rules;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@ApplicationScoped
public class CommanderGameChangerService {

    private static final Logger LOG = Logger.getLogger(CommanderGameChangerService.class);
    private static final String RESOURCE = "rules/commander-game-changers.json";

    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final AtomicReference<GameChangerSnapshot> snapshot = new AtomicReference<>();

    public boolean isGameChanger(String cardName) {
        if (cardName == null || cardName.isBlank()) {
            return false;
        }
        return load().normalizedCards().contains(normalize(cardName));
    }

    public GameChangerSnapshot load() {
        GameChangerSnapshot current = snapshot.get();
        if (current != null) {
            return current;
        }

        GameChangerSnapshot loaded = readSnapshot();
        snapshot.compareAndSet(null, loaded);
        return snapshot.get();
    }

    private GameChangerSnapshot readSnapshot() {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                LOG.errorv("event=commander.game_changers.missing resource={0}", RESOURCE);
                return GameChangerSnapshot.empty();
            }
            GameChangerFile file = mapper.readValue(stream, GameChangerFile.class);
            Set<String> normalized = file.gameChangers().stream()
                    .map(this::normalize)
                    .filter(name -> !name.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
            LOG.infov(
                    "event=commander.game_changers.loaded source={0} effectiveDate={1} cards={2}",
                    file.source(),
                    file.effectiveDate(),
                    normalized.size()
            );
            return new GameChangerSnapshot(file.source(), file.effectiveDate(), file.bracketVersion(), normalized);
        } catch (Exception exception) {
            LOG.errorv(exception, "event=commander.game_changers.load_failed resource={0}", RESOURCE);
            return GameChangerSnapshot.empty();
        }
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public record GameChangerSnapshot(String source, String effectiveDate, String bracketVersion, Set<String> normalizedCards) {
        static GameChangerSnapshot empty() {
            return new GameChangerSnapshot("missing", null, "unknown", Set.of());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GameChangerFile(String source, String effectiveDate, String bracketVersion, List<String> gameChangers) {
        public GameChangerFile {
            gameChangers = gameChangers == null ? List.of() : List.copyOf(gameChangers);
        }
    }
}
