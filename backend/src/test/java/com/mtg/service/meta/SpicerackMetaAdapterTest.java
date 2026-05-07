package com.mtg.service.meta;

import com.mtg.client.SpicerackClient;
import com.mtg.client.SpicerackStandingDTO;
import com.mtg.client.SpicerackTournamentDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpicerackMetaAdapterTest {

    @Test
    void syncFetchesCommanderDecksIntoLocalCache() {
        SpicerackMetaAdapter adapter = new SpicerackMetaAdapter();
        adapter.client = new FakeSpicerackClient();
        adapter.normalizer = new DecklistNormalizer();
        adapter.enabled = true;
        adapter.apiKey = Optional.empty();
        adapter.numDays = 30;
        adapter.maxTournaments = 10;

        var imported = adapter.sync();

        assertEquals(1, imported.size());
        assertEquals("Atraxa, Praetors' Voice", imported.getFirst().commander());
        assertEquals(1, imported.getFirst().cards().size());
        assertEquals("Sol Ring", imported.getFirst().cards().getFirst().name());
        assertEquals(1, adapter.fetchDecks("mid").size());
        assertEquals(0, adapter.fetchDecks("casual").size());
    }

    static class FakeSpicerackClient implements SpicerackClient {
        @Override
        public List<SpicerackTournamentDTO> exportDecklists(
                String apiKey,
                int numDays,
                String eventFormat,
                boolean decklistAsText
        ) {
            assertEquals("COMMANDER2", eventFormat);
            return List.of(new SpicerackTournamentDTO(
                    "tournament-1",
                    "Commander Night",
                    "COMMANDER2",
                    "https://spicerack.gg/events/tournament-1",
                    32,
                    1_700_000_000L,
                    List.of(new SpicerackStandingDTO(
                            "Player",
                            "https://moxfield.com/decks/example",
                            4,
                            1,
                            0,
                            0,
                            0,
                            """
                                    Commander
                                    1 Atraxa, Praetors' Voice

                                    Deck
                                    1 Sol Ring
                                    """
                    ))
            ));
        }
    }
}
