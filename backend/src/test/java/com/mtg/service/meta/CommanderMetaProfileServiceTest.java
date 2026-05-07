package com.mtg.service.meta;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommanderMetaProfileServiceTest {

    @Test
    void buildsProfilesGroupedByCommanderAndBracket() {
        CommanderMetaProfileService service = new CommanderMetaProfileService();
        service.bracketMetaPolicy = new BracketMetaPolicy();

        List<CommanderMetaProfile> profiles = service.buildProfiles(List.of(
                deck("Xenagos, God of Revels", "mid", "Sol Ring", "Arcane Signet"),
                deck("Xenagos, God of Revels", "mid", "Sol Ring", "Beast Within"),
                deck("Xenagos, God of Revels", "cedh", "Mana Crypt"),
                deck("Atraxa, Praetors' Voice", "mid", "Sol Ring")
        ));

        assertEquals(3, profiles.size());

        CommanderMetaProfile xenagosMid = profiles.stream()
                .filter(profile -> profile.commander().equals("Xenagos, God of Revels"))
                .filter(profile -> profile.bracket().equals("mid"))
                .findFirst()
                .orElseThrow();

        assertEquals(2, xenagosMid.sampleSize());
        assertEquals("Sol Ring", xenagosMid.topCards().getFirst().getName());
        assertEquals(2, xenagosMid.topCards().getFirst().getCount());
        assertEquals(1.0, xenagosMid.topCards().getFirst().getInclusion());
        assertEquals(0.5, xenagosMid.topCards().get(1).getInclusion());
    }

    @Test
    void countsCardOnlyOncePerDeckForInclusionRate() {
        CommanderMetaProfileService service = new CommanderMetaProfileService();
        service.bracketMetaPolicy = new BracketMetaPolicy();

        List<CommanderMetaProfile> profiles = service.buildProfiles(List.of(
                new MetaDeck(
                        "test",
                        "1",
                        "Xenagos, God of Revels",
                        List.of(),
                        List.of(),
                        "mid",
                        List.of(),
                        List.of(
                                card("Forest", 10),
                                card("Sol Ring", 1)
                        ),
                        null,
                        null,
                        null,
                        null,
                        null,
                        OffsetDateTime.now()
                ),
                deck("Xenagos, God of Revels", "mid", "Sol Ring")
        ));

        CommanderMetaProfile profile = profiles.getFirst();
        MetaCard forest = profile.topCards().stream()
                .filter(card -> card.getName().equals("Forest"))
                .findFirst()
                .orElseThrow();

        assertEquals(1, forest.getCount());
        assertEquals(0.5, forest.getInclusion());
    }

    @Test
    void ignoresDecksWithoutCommanderOrCards() {
        CommanderMetaProfileService service = new CommanderMetaProfileService();
        service.bracketMetaPolicy = new BracketMetaPolicy();

        List<CommanderMetaProfile> profiles = service.buildProfiles(List.of(
                deck(null, "mid", "Sol Ring"),
                new MetaDeck("test", "2", "Xenagos, God of Revels", List.of(), List.of(), "mid", List.of(), List.of(), null, null, null, null, null, OffsetDateTime.now()),
                deck("Xenagos, God of Revels", "mid", "Sol Ring")
        ));

        assertEquals(1, profiles.size());
        assertEquals(1, profiles.getFirst().sampleSize());
    }

    private MetaDeck deck(String commander, String bracket, String... cards) {
        return new MetaDeck(
                "test",
                commander + ":" + bracket,
                commander,
                List.of(),
                List.of(),
                bracket,
                List.of(),
                List.of(cards).stream().map(name -> card(name, 1)).toList(),
                null,
                null,
                null,
                null,
                null,
                OffsetDateTime.now()
        );
    }

    private MetaDeckCard card(String name, int quantity) {
        return new MetaDeckCard(name, quantity, List.of(), null, null, List.of());
    }
}
