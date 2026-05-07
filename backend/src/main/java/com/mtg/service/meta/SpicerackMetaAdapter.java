package com.mtg.service.meta;

import com.mtg.client.SpicerackClient;
import com.mtg.client.SpicerackStandingDTO;
import com.mtg.client.SpicerackTournamentDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SpicerackMetaAdapter extends AbstractCachedMetaSourceAdapter {
    private static final Logger LOG = Logger.getLogger(SpicerackMetaAdapter.class);
    private static final String SOURCE = "Spicerack";
    private static final String COMMANDER_FORMAT = "COMMANDER2";

    @Inject
    @RestClient
    SpicerackClient client;

    @Inject
    DecklistNormalizer normalizer;

    @ConfigProperty(name = "meta.spicerack.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "meta.spicerack.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "meta.spicerack.num-days", defaultValue = "30")
    int numDays;

    @ConfigProperty(name = "meta.spicerack.max-tournaments", defaultValue = "10")
    int maxTournaments;

    private volatile List<MetaDeck> cachedDecks = List.of();
    private volatile OffsetDateTime lastSync;

    @Override
    public String sourceName() {
        return SOURCE;
    }

    @Override
    public List<String> supportedBrackets() {
        return List.of("mid", "high-power", "cedh");
    }

    @Override
    public List<MetaDeck> sync() {
        if (!enabled) {
            LOG.infov("event=meta.source.disabled source={0}", SOURCE);
            return cachedDecks;
        }

        LOG.infov("event=meta.sync.started source={0} numDays={1}", SOURCE, numDays);
        List<SpicerackTournamentDTO> tournaments = client.exportDecklists(
                apiKey.orElse(null),
                numDays,
                COMMANDER_FORMAT,
                true
        );

        List<MetaDeck> imported = toMetaDecks(tournaments);
        cachedDecks = imported;
        lastSync = OffsetDateTime.now();
        LOG.infov("event=meta.sync.completed source={0} decks={1}", SOURCE, imported.size());
        return imported;
    }

    @Override
    public List<MetaDeck> fetchDecks(String bracket) {
        String normalizedBracket = normalizeBracket(bracket);
        if (normalizedBracket == null) {
            return cachedDecks;
        }
        return cachedDecks.stream()
                .filter(deck -> normalizedBracket.equals(deck.bracket()))
                .toList();
    }

    @Override
    public MetaSourceStatus status() {
        return new MetaSourceStatus(SOURCE, enabled, lastSync, supportedBrackets(), "competitive_meta");
    }

    private List<MetaDeck> toMetaDecks(List<SpicerackTournamentDTO> tournaments) {
        if (tournaments == null || tournaments.isEmpty()) {
            return List.of();
        }

        List<MetaDeck> decks = new ArrayList<>();
        for (SpicerackTournamentDTO tournament : tournaments.stream().limit(maxTournaments).toList()) {
            if (tournament.standings() == null) {
                continue;
            }
            for (int index = 0; index < tournament.standings().size(); index++) {
                SpicerackStandingDTO standing = tournament.standings().get(index);
                MetaDeck deck = toMetaDeck(tournament, standing, index + 1);
                if (deck != null && !deck.cards().isEmpty()) {
                    decks.add(deck);
                }
            }
        }
        return decks;
    }

    private MetaDeck toMetaDeck(SpicerackTournamentDTO tournament, SpicerackStandingDTO standing, int placement) {
        String decklistText = standing.decklistText();
        if (decklistText == null || decklistText.isBlank()) {
            return null;
        }

        String commander = normalizer.findCommander(decklistText);
        List<MetaDeckCard> cards = normalizer.normalizePlainText(decklistText).stream()
                .filter(card -> commander == null || !commander.equalsIgnoreCase(card.name()))
                .toList();
        String externalId = tournament.id() + ":" + placement + ":" + nullSafe(standing.name());

        return new MetaDeck(
                SOURCE,
                externalId,
                commander,
                List.of(),
                List.of(),
                "mid",
                List.of(),
                cards,
                tournament.tournamentName(),
                toDate(tournament.startDate()),
                placement,
                tournament.players(),
                firstPresent(standing.decklist(), tournament.bracketUrl()),
                OffsetDateTime.now()
        );
    }

    private LocalDate toDate(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private String firstPresent(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeBracket(String bracket) {
        if (bracket == null || bracket.isBlank()) {
            return null;
        }
        String normalized = bracket.trim().toLowerCase().replace("_", "-");
        return normalized.equals("highpower") ? "high-power" : normalized;
    }
}
