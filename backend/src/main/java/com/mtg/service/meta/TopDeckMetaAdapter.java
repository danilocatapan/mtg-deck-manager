package com.mtg.service.meta;

import com.mtg.client.TopDeckClient;
import com.mtg.client.TopDeckStandingDTO;
import com.mtg.client.TopDeckTournamentDTO;
import com.mtg.client.TopDeckTournamentRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class TopDeckMetaAdapter extends AbstractCachedMetaSourceAdapter {
    private static final Logger LOG = Logger.getLogger(TopDeckMetaAdapter.class);
    private static final String SOURCE = "TopDeck";

    @Inject
    @RestClient
    TopDeckClient client;

    @Inject
    DecklistNormalizer normalizer;

    @ConfigProperty(name = "meta.topdeck.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "meta.topdeck.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "meta.topdeck.days", defaultValue = "90")
    int days;

    @ConfigProperty(name = "meta.topdeck.min-participants", defaultValue = "32")
    int minParticipants;

    @ConfigProperty(name = "meta.topdeck.max-tournaments", defaultValue = "10")
    int maxTournaments;

    private volatile List<MetaDeck> cachedDecks = List.of();
    private volatile OffsetDateTime lastSync;

    @Override
    public String sourceName() {
        return SOURCE;
    }

    @Override
    public List<String> supportedBrackets() {
        return List.of("high-power", "cedh");
    }

    @Override
    public List<MetaDeck> sync() {
        if (!enabled) {
            LOG.infov("event=meta.source.disabled source={0}", SOURCE);
            return cachedDecks;
        }
        if (apiKey.isEmpty() || apiKey.get().isBlank()) {
            LOG.infov("event=meta.sync.skipped source={0} reason=missing_api_key", SOURCE);
            return cachedDecks;
        }

        LOG.infov("event=meta.sync.started source={0} days={1} minParticipants={2}", SOURCE, days, minParticipants);
        List<TopDeckTournamentDTO> tournaments;
        try {
            tournaments = client.tournaments(
                    apiKey.get(),
                    new TopDeckTournamentRequestDTO(
                            "Magic: The Gathering",
                            "EDH",
                            days,
                            minParticipants,
                            List.of("name", "commander", "deckObj", "decklist", "wins", "draws", "losses", "winRate")
                    )
            );
        } catch (WebApplicationException exception) {
            int status = exception.getResponse() == null ? 0 : exception.getResponse().getStatus();
            LOG.warnv(exception, "event=meta.sync.failed source={0} httpStatus={1}", SOURCE, status);
            return cachedDecks;
        } catch (RuntimeException exception) {
            LOG.warnv(exception, "event=meta.sync.failed source={0}", SOURCE);
            return cachedDecks;
        }
        List<MetaDeck> imported = toMetaDecks(tournaments);
        cachedDecks = imported;
        lastSync = OffsetDateTime.now();
        LOG.infov("event=meta.sync.completed source={0} decks={1}", SOURCE, imported.size());
        return imported;
    }

    @Override
    public List<MetaDeck> fetchDecks(String bracket) {
        if (bracket == null || bracket.isBlank()) {
            return cachedDecks;
        }
        String normalized = bracket.trim().toLowerCase().replace("_", "-");
        return cachedDecks.stream()
                .filter(deck -> normalized.equals(deck.bracket()))
                .toList();
    }

    @Override
    public MetaSourceStatus status() {
        return new MetaSourceStatus(SOURCE, enabled && apiKey.isPresent() && !apiKey.get().isBlank(), lastSync, supportedBrackets(), "competitive_meta");
    }

    private List<MetaDeck> toMetaDecks(List<TopDeckTournamentDTO> tournaments) {
        if (tournaments == null || tournaments.isEmpty()) {
            return List.of();
        }

        List<MetaDeck> decks = new ArrayList<>();
        for (TopDeckTournamentDTO tournament : tournaments.stream().limit(maxTournaments).toList()) {
            if (tournament.standings() == null) {
                continue;
            }
            for (int index = 0; index < tournament.standings().size(); index++) {
                MetaDeck deck = toMetaDeck(tournament, tournament.standings().get(index), index + 1);
                if (deck != null && !deck.cards().isEmpty()) {
                    decks.add(deck);
                }
            }
        }
        return decks;
    }

    private MetaDeck toMetaDeck(TopDeckTournamentDTO tournament, TopDeckStandingDTO standing, int placement) {
        boolean hasDecklist = standing.decklist() != null && !standing.decklist().isBlank();
        boolean hasDeckObj = standing.deckObj() != null && !standing.deckObj().isEmpty();
        if (!hasDecklist && !hasDeckObj) {
            return null;
        }
        String commander = firstNonBlank(standing.commander(), hasDecklist ? normalizer.findCommander(standing.decklist()) : null);
        if ((commander == null || commander.isBlank()) && !hasDecklist) {
            return null;
        }
        List<MetaDeckCard> cards = (hasDeckObj ? fromDeckObj(standing.deckObj()) : normalizer.normalizePlainText(standing.decklist())).stream()
                .filter(card -> commander == null || !commander.equalsIgnoreCase(card.name()))
                .toList();
        return new MetaDeck(
                SOURCE,
                nullSafe(tournament.id()) + ":" + placement + ":" + nullSafe(standing.name()),
                commander,
                List.of(),
                List.of(),
                bracketFor(tournament, placement),
                List.of(),
                cards,
                tournament.tournamentName(),
                toDate(tournament.startDate()),
                placement,
                tournament.participantCount(),
                tournament.topdeckUrl(),
                OffsetDateTime.now()
        );
    }

    private String bracketFor(TopDeckTournamentDTO tournament, int placement) {
        int players = tournament.participantCount() == null ? 0 : tournament.participantCount();
        return placement <= 16 && players >= minParticipants ? "cedh" : "high-power";
    }

    private LocalDate toDate(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private List<MetaDeckCard> fromDeckObj(Map<String, Integer> deckObj) {
        if (deckObj == null || deckObj.isEmpty()) {
            return List.of();
        }
        return deckObj.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .map(entry -> new MetaDeckCard(entry.getKey().trim(), Math.max(1, entry.getValue() == null ? 1 : entry.getValue()), List.of(), null, null, List.of()))
                .toList();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
