package com.mtg.service.meta;

import com.mtg.client.TopDeckClient;
import com.mtg.client.TopDeckStandingDTO;
import com.mtg.client.TopDeckTournamentDTO;
import com.mtg.client.TopDeckTournamentRequestDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TopDeckMetaAdapterTest {

    @Test
    void syncFetchesCompetitiveDecksIntoLocalCache() {
        TopDeckMetaAdapter adapter = new TopDeckMetaAdapter();
        adapter.client = new FakeTopDeckClient();
        adapter.normalizer = new DecklistNormalizer();
        adapter.enabled = true;
        adapter.apiKey = Optional.of("test-key");
        adapter.days = 90;
        adapter.minParticipants = 32;
        adapter.maxTournaments = 10;

        List<MetaDeck> imported = adapter.sync();

        assertEquals(1, imported.size());
        assertEquals("TopDeck", imported.getFirst().source());
        assertEquals("Tymna the Weaver", imported.getFirst().commander());
        assertEquals("cedh", imported.getFirst().bracket());
        assertEquals(64, imported.getFirst().playerCount());
        assertEquals(1, imported.getFirst().placement());
        assertEquals("Thassa's Oracle", imported.getFirst().cards().getFirst().name());
        assertEquals(1, adapter.fetchDecks("cedh").size());
        assertEquals(0, adapter.fetchDecks("casual").size());
    }

    static class FakeTopDeckClient implements TopDeckClient {
        @Override
        public List<TopDeckTournamentDTO> tournaments(String apiKey, TopDeckTournamentRequestDTO request) {
            assertEquals("test-key", apiKey);
            assertEquals("Magic: The Gathering", request.game());
            assertEquals("EDH", request.format());
            return List.of(new TopDeckTournamentDTO(
                    "event-1",
                    "cEDH Open",
                    "EDH",
                    64,
                    1_700_000_000L,
                    "https://topdeck.gg/event-1",
                    List.of(new TopDeckStandingDTO(
                            "Player",
                            """
                                    ~~Commanders~~
                                    1 Tymna the Weaver

                                    ~~Mainboard~~
                                    1 Thassa's Oracle
                                    1 Demonic Consultation
                                    """,
                            5,
                            0,
                            0,
                            1.0
                    ))
            ));
        }
    }
}
