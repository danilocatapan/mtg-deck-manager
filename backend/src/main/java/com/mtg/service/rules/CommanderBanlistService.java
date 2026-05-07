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
public class CommanderBanlistService {

    private static final Logger LOG = Logger.getLogger(CommanderBanlistService.class);
    private static final String RESOURCE = "rules/commander-banlist.json";

    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final AtomicReference<CommanderBanlistSnapshot> snapshot = new AtomicReference<>();

    public boolean isBanned(String cardName) {
        if (cardName == null || cardName.isBlank()) {
            return false;
        }
        return load().normalizedCards().contains(normalize(cardName));
    }

    public CommanderBanlistSnapshot load() {
        CommanderBanlistSnapshot current = snapshot.get();
        if (current != null) {
            return current;
        }

        CommanderBanlistSnapshot loaded = readSnapshot();
        snapshot.compareAndSet(null, loaded);
        return snapshot.get();
    }

    private CommanderBanlistSnapshot readSnapshot() {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                LOG.errorv("event=commander.banlist.missing resource={0}", RESOURCE);
                return CommanderBanlistSnapshot.empty();
            }
            CommanderBanlistFile file = mapper.readValue(stream, CommanderBanlistFile.class);
            Set<String> normalized = file.bannedCards().stream()
                    .map(this::normalize)
                    .filter(name -> !name.isBlank())
                    .collect(Collectors.toUnmodifiableSet());
            LOG.infov(
                    "event=commander.banlist.loaded source={0} effectiveDate={1} cards={2}",
                    file.source(),
                    file.effectiveDate(),
                    normalized.size()
            );
            return new CommanderBanlistSnapshot(file.source(), file.effectiveDate(), normalized);
        } catch (Exception exception) {
            LOG.errorv(exception, "event=commander.banlist.load_failed resource={0}", RESOURCE);
            return CommanderBanlistSnapshot.empty();
        }
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public record CommanderBanlistSnapshot(String source, String effectiveDate, Set<String> normalizedCards) {
        static CommanderBanlistSnapshot empty() {
            return new CommanderBanlistSnapshot("missing", null, Set.of());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommanderBanlistFile(String source, String effectiveDate, List<String> bannedCards) {
        public CommanderBanlistFile {
            bannedCards = bannedCards == null ? List.of() : List.copyOf(bannedCards);
        }
    }
}
