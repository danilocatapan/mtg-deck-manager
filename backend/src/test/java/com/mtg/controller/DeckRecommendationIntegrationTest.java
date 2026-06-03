package com.mtg.controller;

import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.CardLookupRequestDTO;
import com.mtg.dto.DeckImportDTO;
import com.mtg.model.RecommendationAuditRun;
import com.mtg.repository.RecommendationAuditRepository;
import com.mtg.service.CardService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
@TestProfile(DeckRecommendationIntegrationTest.RecommendationFixtureProfile.class)
class DeckRecommendationIntegrationTest {

    private static final Set<String> GRAND_ARBITER_COLORS = Set.of("W", "U");
    private static final Set<String> XENAGOS_COLORS = Set.of("R", "G");
    private static final Set<String> KRRRIK_COLORS = Set.of("B");

    @InjectMock
    CardService cardService;

    @Inject
    RecommendationAuditRepository auditRepository;

    @BeforeEach
    void setupCards() {
        Mockito.when(cardService.normalizeLookupName(anyString())).thenAnswer(invocation -> normalize(invocation.getArgument(0)));
        Mockito.when(cardService.findByNames(any())).thenAnswer(invocation -> resolvedCards(invocation.getArgument(0)));
        Mockito.when(cardService.findByCardRequests(any())).thenAnswer(invocation -> resolvedLookupCards(invocation.getArgument(0)));
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void importedDeckListWithCommanderLinePersistsMainDeckWithoutCommander() {
        String content = fixture("decklists/grand-arbiter-augustin-iv.txt");

        given()
                .contentType(ContentType.JSON)
                .body(new DeckImportDTO("Grand Arbiter imported fixture", "Grand Arbiter Augustin IV", content))
                .when().post("/decks/import")
                .then()
                .statusCode(201)
                .body("commander", is("Grand Arbiter Augustin IV"))
                .body("colorIdentity", is("WU"))
                .body("cards.name", not(org.hamcrest.Matchers.hasItem("Grand Arbiter Augustin IV")))
                .body("cards.quantity.sum()", is(99));
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void seededGrandArbiterDeckReturnsValidStrategicRecommendations() {
        String deckLocation = importFixtureDeck(
                "Grand Arbiter recommendation fixture",
                "Grand Arbiter Augustin IV",
                "decklists/grand-arbiter-augustin-iv.txt"
        );

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("bracket", "casual"))
                .when().post(deckLocation + "/recommendations/strategic")
                .then()
                .statusCode(200)
                .body("confidence", not(is("")))
                .body("coverage.commanderKnown", is(true))
                .body("recommendations", hasSize(greaterThanOrEqualTo(3)))
                .body("recommendations.add", everyItem(not(is("Grand Arbiter Augustin IV"))))
                .body("recommendations.remove", everyItem(not(is("Grand Arbiter Augustin IV"))))
                .body("recommendations.reasoning", everyItem(not(is(""))))
                .body("recommendations.budget", everyItem(nullValue()));
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void seededXenagosDeckReturnsValidStrategicRecommendations() {
        String deckLocation = importFixtureDeck(
                "Xenagos recommendation fixture",
                "Xenagos, God of Revels",
                "decklists/xenagos-god-of-revels.txt"
        );

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("bracket", "casual"))
                .when().post(deckLocation + "/recommendations/strategic")
                .then()
                .statusCode(200)
                .body("confidence", not(is("")))
                .body("coverage.commanderKnown", is(true))
                .body("recommendations", hasSize(greaterThanOrEqualTo(3)))
                .body("recommendations.add", everyItem(not(is("Xenagos, God of Revels"))))
                .body("recommendations.remove", everyItem(not(is("Xenagos, God of Revels"))))
                .body("recommendations.reasoning", everyItem(not(is(""))))
                .body("recommendations.budget", everyItem(nullValue()));
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void seededXenagosHighPowerDeckReturnsStrategicQualityRecommendations() {
        String deckLocation = importFixtureDeck(
                "Xenagos high-power benchmark fixture",
                "Xenagos, God of Revels",
                "decklists/xenagos-god-of-revels.txt"
        );

        io.restassured.path.json.JsonPath response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "bracket", "high-power",
                        "strategy", "competitive",
                        "lowerCurve", true,
                        "improveMana", true,
                        "maxRecommendations", 10
                ))
                .when().post(deckLocation + "/recommendations/strategic")
                .then()
                .statusCode(200)
                .body("confidence", not(is("")))
                .body("coverage.commanderKnown", is(true))
                .body("benchmarkStatus", not(is("")))
                .body("recommendations", hasSize(greaterThanOrEqualTo(5)))
                .body("recommendations.add", everyItem(not(is("Xenagos, God of Revels"))))
                .body("recommendations.remove", everyItem(not(is("Xenagos, God of Revels"))))
                .body("recommendations.reasoning", everyItem(not(is(""))))
                .extract()
                .jsonPath();

        List<String> adds = response.getList("recommendations.add", String.class);
        List<String> cuts = response.getList("recommendations.remove", String.class);
        List<Map<String, Object>> addBreakdowns = response.getList("recommendations.addScoreBreakdown");
        List<Map<String, Object>> cutBreakdowns = response.getList("recommendations.cutScoreBreakdown");

        Set<String> expectedAdds = Set.of(
                "Savage Ventmaw",
                "Bloodthirster",
                "Scourge of the Throne",
                "Hellkite Charger",
                "Finale of Devastation",
                "Greater Good",
                "Worldly Tutor",
                "Utopia Sprawl",
                "Pathbreaker Ibex",
                "Moraug, Fury of Akoum",
                "Old Gnawbone",
                "Etali, Primal Conqueror"
        );
        Set<String> expectedCuts = Set.of(
                "Terastodon",
                "Thran Dynamo",
                "Nissa, Who Shakes the World",
                "Garruk, Primal Hunter",
                "Siege Behemoth",
                "Cultivate",
                "Nissa's Pilgrimage",
                "Temple of Abandon",
                "Rugged Highlands",
                "Soul's Majesty",
                "Hunter's Insight",
                "Gruul Turf"
        );

        assertTrue(adds.stream().filter(expectedAdds::contains).count() >= 4);
        assertTrue(cuts.stream().filter(expectedCuts::contains).count() >= 4);
        assertTrue(adds.stream().noneMatch(add -> Set.of("Marble Diamond", "Sky Diamond").contains(add)));
        assertTrue(cuts.stream().noneMatch(cut -> cut.equals("Atarka, World Render")));
        assertTrue(adds.stream().noneMatch(cuts::contains));
        assertTrue(addBreakdowns.stream().allMatch(breakdown -> breakdown != null && breakdown.containsKey("multiplayerScore")));
        assertTrue(cutBreakdowns.stream().allMatch(breakdown -> breakdown != null && breakdown.containsKey("strategicKeepValue")));

        RecommendationAuditRun audit = auditRepository.listByDeckAndOwner(deckIdFromLocation(deckLocation), "google-user-1").getFirst();
        assertTrue(Set.of("voltron", "combat").contains(audit.getArchetype()));
        assertTrue(audit.getRecommendationsJson().contains("addScoreBreakdown"));
        assertTrue(audit.getBlockedPairsJson().contains("reason") || audit.getBlockedPairsJson().equals("[]"));
        assertTrue(audit.getProtectedCutsJson().contains("Atarka, World Render") || audit.getProtectedCutsJson().contains("protected"));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "needs_review", "reason", "benchmark", "notes", "Auditoria validada pelo teste."))
                .when().post("/recommendation-audits/" + audit.getId() + "/feedback")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "google-user-1")
    void seededKrrikCedhDeckReturnsBenchmarkAlignedRecommendations() {
        String deckLocation = importFixtureDeck(
                "Krrik cEDH benchmark fixture",
                "K'rrik, Son of Yawgmoth",
                "decklists/krrik-son-of-yawgmoth.txt"
        );

        io.restassured.path.json.JsonPath response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "bracket", "cedh",
                        "strategy", "competitive",
                        "maxRecommendations", 10
                ))
                .when().post(deckLocation + "/recommendations/strategic")
                .then()
                .statusCode(200)
                .body("confidence", not(is("")))
                .body("coverage.commanderKnown", is(true))
                .body("recommendations", hasSize(10))
                .body("recommendations.add", everyItem(not(is("K'rrik, Son of Yawgmoth"))))
                .body("recommendations.remove", everyItem(not(is("K'rrik, Son of Yawgmoth"))))
                .body("recommendations.reasoning", everyItem(not(is(""))))
                .extract()
                .jsonPath();

        List<String> adds = response.getList("recommendations.add", String.class);
        List<String> cuts = response.getList("recommendations.remove", String.class);

        Set<String> expectedAdds = Set.of(
                "Cabal Ritual",
                "Demonic Tutor",
                "Vampiric Tutor",
                "Mana Vault",
                "Ancient Tomb",
                "Necropotence",
                "Chrome Mox",
                "Imperial Seal",
                "Grim Monolith",
                "Mox Diamond",
                "Buried Alive",
                "Sacrifice",
                "Rain of Filth",
                "Shallow Grave",
                "Snuff Out",
                "Demonic Consultation",
                "Lotus Petal",
                "Lion's Eye Diamond"
        );
        Set<String> expectedCuts = Set.of(
                "Temple of the False God",
                "Memorial to Folly",
                "Mortuary Mire",
                "Charcoal Diamond",
                "Coldsteel Heart",
                "Phyrexian Arena",
                "Underworld Dreams",
                "Painful Quandary",
                "Grave Titan",
                "Massacre Wurm",
                "Diabolic Tutor",
                "The Sibsig Ceremony",
                "Feed the Cycle"
        );

        assertTrue(adds.stream().filter(expectedAdds::contains).count() >= 6);
        assertTrue(cuts.stream().filter(expectedCuts::contains).count() >= 4);
        assertTrue(adds.stream().noneMatch(add -> Set.of("Thassa's Oracle", "Mystic Remora", "Rhystic Study", "Swan Song", "Flusterstorm").contains(add)));
        assertTrue(adds.stream().noneMatch(cuts::contains));
        assertTrue(cuts.stream().noneMatch(cut -> cut.equals("Vilis, Broker of Blood")));

        RecommendationAuditRun audit = auditRepository.listByDeckAndOwner(deckIdFromLocation(deckLocation), "google-user-1").getFirst();
        assertEquals("turbo-combo", audit.getArchetype());
        assertTrue(audit.getRecommendationsJson().contains("addScoreBreakdown"));
        assertTrue(audit.getBlockedPairsJson().contains("reason") || audit.getBlockedPairsJson().equals("[]"));
    }

    private String importFixtureDeck(String name, String commander, String fixturePath) {
        return given()
                .contentType(ContentType.JSON)
                .body(new DeckImportDTO(name, commander, fixture(fixturePath)))
                .when().post("/decks/import")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
    }

    private Long deckIdFromLocation(String deckLocation) {
        return Long.parseLong(deckLocation.substring(deckLocation.lastIndexOf('/') + 1));
    }

    private Map<String, CardResponseDTO> resolvedCards(List<String> names) {
        Map<String, CardResponseDTO> cards = new LinkedHashMap<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            CardResponseDTO card = cardFor(name.trim());
            cards.put(normalize(card.name()), card);
        }
        return cards;
    }

    private Map<String, CardResponseDTO> resolvedLookupCards(List<CardLookupRequestDTO> lookups) {
        Map<String, CardResponseDTO> cards = new LinkedHashMap<>();
        for (CardLookupRequestDTO lookup : lookups) {
            if (lookup == null || lookup.name() == null || lookup.name().isBlank()) {
                continue;
            }
            CardResponseDTO base = cardFor(lookup.name().trim());
            cards.put(lookup.lookupKey(), new CardResponseDTO(
                    base.name(),
                    base.manaCost(),
                    base.typeLine(),
                    base.oracleText(),
                    base.cmc(),
                    base.colorIdentity(),
                    base.keywords(),
                    null,
                    null,
                    "scryfall-" + normalize(base.name()),
                    lookup.setCode(),
                    lookup.setCode() == null ? null : "Test Set",
                    lookup.collectorNumber(),
                    List.of("nonfoil")
            ));
        }
        return cards;
    }

    private CardResponseDTO cardFor(String name) {
        String normalized = normalize(name);
        return switch (normalized) {
            case "grand arbiter augustin iv" -> card(name, "{2}{W}{U}", "Legendary Creature - Human Advisor", "White spells you cast cost {1} less to cast. Blue spells you cast cost {1} less to cast. Spells your opponents cast cost {1} more to cast.", 4.0, "W", "U");
            case "xenagos, god of revels" -> card(name, "{3}{R}{G}", "Legendary Enchantment Creature - God", "At the beginning of combat on your turn, another target creature you control gains haste and trample and gets +X/+X until end of turn.", 5.0, "R", "G");
            case "k'rrik, son of yawgmoth" -> card(name, "{4}{B/P}{B/P}{B/P}", "Legendary Creature - Phyrexian Horror Minion", "For each {B} in a cost, you may pay 2 life rather than pay that mana. Whenever you cast a black spell, put a +1/+1 counter on K'rrik.", 7.0, "B");
            case "cabal ritual" -> card(name, "{1}{B}", "Instant", "Add {B}{B}{B}. Threshold - Add {B}{B}{B}{B}{B} instead if seven or more cards are in your graveyard.", 2.0, "B");
            case "demonic tutor" -> card(name, "{1}{B}", "Sorcery", "Search your library for a card, put that card into your hand, then shuffle.", 2.0, "B");
            case "vampiric tutor" -> card(name, "{B}", "Instant", "Search your library for a card, then shuffle and put that card on top. You lose 2 life.", 1.0, "B");
            case "mana vault" -> card(name, "{1}", "Artifact", "{T}: Add {C}{C}{C}. This artifact doesn't untap during your untap step.", 1.0);
            case "ancient tomb" -> card(name, "", "Land", "{T}: Add {C}{C}. This land deals 2 damage to you.", 0.0);
            case "necropotence" -> card(name, "{B}{B}{B}", "Enchantment", "Skip your draw step. Pay 1 life: Exile the top card of your library face down. Put that card into your hand at the beginning of your next end step.", 3.0, "B");
            case "chrome mox" -> card(name, "{0}", "Artifact", "Imprint. {T}: Add one mana of any of the exiled card's colors.", 0.0);
            case "imperial seal" -> card(name, "{B}", "Sorcery", "Search your library for a card, then shuffle and put that card on top. You lose 2 life.", 1.0, "B");
            case "grim monolith" -> card(name, "{2}", "Artifact", "{T}: Add {C}{C}{C}. This artifact doesn't untap during your untap step.", 2.0);
            case "mox diamond" -> card(name, "{0}", "Artifact", "If this artifact would enter, discard a land card instead. {T}: Add one mana of any color.", 0.0);
            case "buried alive" -> card(name, "{2}{B}", "Sorcery", "Search your library for up to three creature cards, put them into your graveyard, then shuffle.", 3.0, "B");
            case "sacrifice" -> card(name, "{B}", "Instant", "As an additional cost to cast this spell, sacrifice a creature. Add an amount of {B} equal to the sacrificed creature's mana value.", 1.0, "B");
            case "vile entomber" -> card(name, "{2}{B}{B}", "Creature - Zombie Warlock", "Deathtouch. When this creature enters, search your library for a card, put that card into your graveyard, then shuffle.", 4.0, "B");
            case "dauthi voidwalker" -> card(name, "{B}{B}", "Creature - Dauthi Rogue", "Shadow. If a card would be put into an opponent's graveyard, exile it with a void counter instead.", 2.0, "B");
            case "rain of filth" -> card(name, "{B}", "Instant", "Until end of turn, lands you control gain Sacrifice this land: Add {B}.", 1.0, "B");
            case "shallow grave" -> card(name, "{1}{B}", "Instant", "Return the top creature card of your graveyard to the battlefield. That creature gains haste. Exile it at the beginning of the next end step.", 2.0, "B");
            case "snuff out" -> card(name, "{3}{B}", "Instant", "If you control a Swamp, you may pay 4 life rather than pay this spell's mana cost. Destroy target nonblack creature.", 4.0, "B");
            case "ad nauseam" -> card(name, "{3}{B}{B}", "Instant", "Reveal the top card of your library and put that card into your hand. You lose life equal to its mana value. You may repeat this process any number of times.", 5.0, "B");
            case "demonic consultation" -> card(name, "{B}", "Instant", "Name a card. Exile cards from the top of your library until you reveal the named card.", 1.0, "B");
            case "lotus petal" -> card(name, "{0}", "Artifact", "{T}, Sacrifice this artifact: Add one mana of any color.", 0.0);
            case "lion's eye diamond" -> card(name, "{0}", "Artifact", "Discard your hand, Sacrifice this artifact: Add three mana of any one color.", 0.0);
            case "nature's lore" -> card(name, "{1}{G}", "Sorcery", "Search your library for a Forest card and put it onto the battlefield.", 2.0, "G");
            case "farseek" -> card(name, "{1}{G}", "Sorcery", "Search your library for a Plains, Island, Swamp, or Mountain card and put it onto the battlefield tapped.", 2.0, "G");
            case "arcane signet" -> card(name, "{2}", "Artifact", "Add one mana of any color in your commander's color identity.", 2.0);
            case "fellwar stone" -> card(name, "{2}", "Artifact", "Add one mana of any color that a land an opponent controls could produce.", 2.0);
            case "marble diamond" -> card(name, "{2}", "Artifact", "Marble Diamond enters the battlefield tapped. Add {W}.", 2.0);
            case "sky diamond" -> card(name, "{2}", "Artifact", "Sky Diamond enters the battlefield tapped. Add {U}.", 2.0);
            case "greater good" -> card(name, "{2}{G}{G}", "Enchantment", "Sacrifice a creature: Draw cards equal to the sacrificed creature's power, then discard three cards.", 4.0, "G");
            case "harmonize" -> card(name, "{2}{G}{G}", "Sorcery", "Draw three cards.", 4.0, "G");
            case "fact or fiction" -> card(name, "{3}{U}", "Instant", "Reveal the top five cards of your library. An opponent separates those cards into two piles. Put one pile into your hand.", 4.0, "U");
            case "windfall" -> card(name, "{2}{U}", "Sorcery", "Each player discards their hand, then draws cards equal to the greatest number of cards a player discarded this way.", 3.0, "U");
            case "archivist" -> card(name, "{2}{U}{U}", "Creature - Human Wizard", "{T}: Draw a card.", 4.0, "U");
            case "beast within" -> card(name, "{2}{G}", "Instant", "Destroy target permanent.", 3.0, "G");
            case "generous gift" -> card(name, "{2}{W}", "Instant", "Destroy target permanent.", 3.0, "W");
            case "chaos warp" -> card(name, "{2}{R}", "Instant", "The owner of target permanent shuffles it into their library.", 3.0, "R");
            case "reality shift" -> card(name, "{1}{U}", "Instant", "Exile target creature.", 2.0, "U");
            case "pongify" -> card(name, "{U}", "Instant", "Destroy target creature. It can't be regenerated.", 1.0, "U");
            case "resculpt" -> card(name, "{1}{U}", "Instant", "Exile target artifact or creature.", 2.0, "U");
            case "heroic intervention" -> card(name, "{1}{G}", "Instant", "Permanents you control gain hexproof and indestructible until end of turn.", 2.0, "G");
            case "swiftfoot boots" -> card(name, "{2}", "Artifact - Equipment", "Equipped creature has hexproof and haste.", 2.0);
            case "flawless maneuver" -> card(name, "{2}{W}", "Instant", "Creatures you control gain indestructible until end of turn.", 3.0, "W");
            case "loran's escape" -> card(name, "{W}", "Instant", "Target artifact or creature gains hexproof and indestructible until end of turn.", 1.0, "W");
            case "slip out the back" -> card(name, "{U}", "Instant", "Put a +1/+1 counter on target creature. It phases out.", 1.0, "U");
            case "utopia sprawl" -> card(name, "{G}", "Enchantment - Aura", "Enchant Forest. Whenever enchanted Forest is tapped for mana, its controller adds one mana of the chosen color.", 1.0, "G");
            case "finale of devastation" -> card(name, "{X}{G}{G}", "Sorcery", "Search your library and/or graveyard for a creature card with mana value X or less and put it onto the battlefield. If X is 10 or more, creatures you control get +X/+X and gain haste until end of turn.", 2.0, "G");
            case "worldly tutor" -> card(name, "{G}", "Instant", "Search your library for a creature card, reveal it, then shuffle and put the card on top.", 1.0, "G");
            case "savage ventmaw" -> card(name, "{4}{R}{G}", "Creature - Dragon", "Flying. Whenever this creature attacks, add {R}{R}{R}{G}{G}{G}.", 6.0, "R", "G");
            case "hellkite charger" -> card(name, "{4}{R}{R}", "Creature - Dragon", "Flying, haste. Whenever this creature attacks, you may pay {5}{R}{R}. If you do, untap all attacking creatures and after this phase, there is an additional combat phase.", 6.0, "R");
            case "bloodthirster" -> card(name, "{5}{R}", "Creature - Demon", "Flying, trample. Whenever this creature deals combat damage to a player, untap it. After this combat phase, there is an additional combat phase.", 6.0, "R");
            case "scourge of the throne" -> card(name, "{4}{R}{R}", "Creature - Dragon", "Flying. Whenever this creature attacks for the first time each turn, untap all attacking creatures. After this phase, there is an additional combat phase.", 6.0, "R");
            case "pathbreaker ibex" -> card(name, "{4}{G}{G}", "Creature - Goat", "Whenever this creature attacks, creatures you control gain trample and get +X/+X until end of turn.", 6.0, "G");
            case "moraug, fury of akoum" -> card(name, "{4}{R}{R}", "Legendary Creature - Minotaur Warrior", "Landfall - if it's your main phase, there is an additional combat phase after this phase.", 6.0, "R");
            case "old gnawbone" -> card(name, "{5}{G}{G}", "Legendary Creature - Dragon", "Flying. Whenever a creature you control deals combat damage to a player, create that many Treasure tokens.", 7.0, "G");
            case "etali, primal conqueror" -> card(name, "{5}{R}{R}", "Legendary Creature - Elder Dinosaur", "When this creature enters, each player exiles cards from the top of their library until they exile a nonland card. You may cast any number of spells from among them.", 7.0, "R");
            case "atarka, world render" -> card(name, "{5}{R}{G}", "Legendary Creature - Dragon", "Flying, trample. Whenever a Dragon you control attacks, it gains double strike until end of turn.", 7.0, "R", "G");
            case "overwhelming stampede" -> card(name, "{3}{G}{G}", "Sorcery", "Creatures you control gain trample and get +X/+X until end of turn.", 5.0, "G");
            case "craterhoof behemoth" -> card(name, "{5}{G}{G}{G}", "Creature - Beast", "When this creature enters, creatures you control gain trample and get +X/+X until end of turn.", 8.0, "G");
            case "approach of the second sun" -> card(name, "{6}{W}", "Sorcery", "If this spell was cast from your hand and you've cast another spell named Approach of the Second Sun this game, you win the game.", 7.0, "W");
            case "shark typhoon" -> card(name, "{5}{U}", "Enchantment", "Whenever you cast a noncreature spell, create an X/X blue Shark creature token with flying.", 6.0, "U");
            case "terastodon" -> card(name, "{6}{G}{G}", "Creature - Elephant", "When this creature enters, you may destroy up to three target noncreature permanents.", 8.0, "G");
            case "siege behemoth" -> card(name, "{5}{G}{G}", "Creature - Beast", "Hexproof. As long as this creature is attacking, each creature you control assigns combat damage as though it weren't blocked.", 7.0, "G");
            case "balefire dragon" -> card(name, "{5}{R}{R}", "Creature - Dragon", "Flying. Whenever this creature deals combat damage to a player, it deals that much damage to each creature that player controls.", 7.0, "R");
            case "nissa, who shakes the world" -> card(name, "{3}{G}{G}", "Legendary Planeswalker - Nissa", "Whenever you tap a Forest for mana, add an additional green. Put three +1/+1 counters on target land you control.", 5.0, "G");
            case "garruk, primal hunter" -> card(name, "{2}{G}{G}{G}", "Legendary Planeswalker - Garruk", "Draw cards equal to the greatest power among creatures you control.", 5.0, "G");
            case "thran dynamo" -> card(name, "{4}", "Artifact", "{T}: Add {C}{C}{C}.", 4.0);
            case "cultivate" -> card(name, "{2}{G}", "Sorcery", "Search your library for up to two basic land cards, reveal those cards, put one onto the battlefield tapped and the other into your hand.", 3.0, "G");
            case "nissa's pilgrimage" -> card(name, "{2}{G}", "Sorcery", "Search your library for up to two basic Forest cards, reveal those cards, put one onto the battlefield tapped and the rest into your hand.", 3.0, "G");
            case "soul's majesty" -> card(name, "{4}{G}", "Sorcery", "Draw cards equal to the power of target creature you control.", 5.0, "G");
            case "hunter's insight" -> card(name, "{2}{G}", "Instant", "Whenever target creature you control deals combat damage to a player or planeswalker this turn, draw that many cards.", 3.0, "G");
            case "temple of abandon" -> card(name, "", "Land", "This land enters tapped. When it enters, scry 1. Add {R} or {G}.", 0.0);
            case "temple of the false god" -> card(name, "", "Land", "Add {C}{C}. Activate only if you control five or more lands.", 0.0);
            case "memorial to folly", "mortuary mire" -> card(name, "", "Land", "This land enters tapped. Return a creature card from your graveyard to your hand.", 0.0);
            case "rugged highlands" -> card(name, "", "Land", "This land enters tapped. When it enters, you gain 1 life. Add {R} or {G}.", 0.0);
            case "gruul turf" -> card(name, "", "Land", "This land enters tapped. When it enters, return a land you control to its owner's hand. Add {R}{G}.", 0.0);
            case "charcoal diamond" -> card(name, "{2}", "Artifact", "This artifact enters tapped. {T}: Add {B}.", 2.0);
            case "coldsteel heart" -> card(name, "{2}", "Snow Artifact", "This artifact enters tapped. As it enters, choose a color. {T}: Add one mana of the chosen color.", 2.0);
            case "phyrexian arena" -> card(name, "{1}{B}{B}", "Enchantment", "At the beginning of your upkeep, you draw a card and you lose 1 life.", 3.0, "B");
            case "underworld dreams" -> card(name, "{B}{B}{B}", "Enchantment", "Whenever an opponent draws a card, this enchantment deals 1 damage to that player.", 3.0, "B");
            case "painful quandary" -> card(name, "{3}{B}{B}", "Enchantment", "Whenever an opponent casts a spell, that player loses 5 life unless they discard a card.", 5.0, "B");
            case "grave titan" -> card(name, "{4}{B}{B}", "Creature - Giant", "Deathtouch. Whenever this creature enters or attacks, create two 2/2 Zombie creature tokens.", 6.0, "B");
            case "massacre wurm" -> card(name, "{3}{B}{B}{B}", "Creature - Phyrexian Wurm", "When this creature enters, creatures your opponents control get -2/-2 until end of turn.", 6.0, "B");
            case "diabolic tutor" -> card(name, "{2}{B}{B}", "Sorcery", "Search your library for a card, put that card into your hand, then shuffle.", 4.0, "B");
            case "vilis, broker of blood" -> card(name, "{5}{B}{B}{B}", "Legendary Creature - Demon", "Flying. Pay {B}, Pay 2 life: Target creature gets -1/-1 until end of turn. Whenever you lose life, draw that many cards.", 8.0, "B");
            case "the sibsig ceremony" -> card(name, "{2}{B}{B}", "Sorcery", "Mill cards, then return a creature card from your graveyard to your hand.", 4.0, "B");
            case "feed the cycle" -> card(name, "{1}{B}", "Instant", "Destroy target creature or planeswalker. You lose 2 life.", 2.0, "B");
            case "path of ancestry", "evolving wilds", "terramorphic expanse" -> card(name, "", "Land", "Add one mana of any color in your commander's color identity.", 0.0);
            default -> inferredCard(name);
        };
    }

    private CardResponseDTO inferredCard(String name) {
        String normalized = normalize(name);
        if (basicLands().contains(normalized) || normalized.contains("tower") || normalized.contains("ruin")
                || normalized.contains("landscape") || normalized.contains("orchard") || normalized.contains("strand")
                || normalized.contains("wastes") || normalized.contains("glade") || normalized.contains("turf")
                || normalized.contains("crag") || normalized.contains("temple") || normalized.contains("bridge")
                || normalized.contains("clinic") || normalized.contains("garenbrig") || normalized.contains("foundry")) {
            return card(name, "", "Land", "Add one mana.", 0.0);
        }
        if (normalized.contains("signet") || normalized.contains("talisman") || normalized.contains("sol ring")
                || normalized.contains("monolith") || normalized.contains("dynamo") || normalized.contains("stone")) {
            return card(name, "{2}", "Artifact", "Add one mana.", 2.0);
        }
        if (normalized.contains("draw") || normalized.contains("study") || normalized.contains("remora")
                || normalized.contains("insight") || normalized.contains("expertise") || normalized.contains("archmage")) {
            return card(name, "{3}", "Spell", "Draw a card.", 3.0, colorsForName(name));
        }
        if (normalized.contains("path to exile") || normalized.contains("swords to plowshares")
                || normalized.contains("counterspell") || normalized.contains("negate") || normalized.contains("rift")
                || normalized.contains("claim") || normalized.contains("breach")) {
            return card(name, "{2}", "Instant", "Exile target creature.", 2.0, colorsForName(name));
        }
        if (normalized.contains("hydra") || normalized.contains("dragon") || normalized.contains("behemoth")
                || normalized.contains("terastodon") || normalized.contains("horror") || normalized.contains("ghalta")) {
            return card(name, "{5}", "Creature", "Trample.", 6.0, colorsForName(name));
        }
        return card(name, "{3}", "Spell", "", 3.0, colorsForName(name));
    }

    private String[] colorsForName(String name) {
        String normalized = normalize(name);
        if (normalized.contains("xenagos") || normalized.contains("gruul") || normalized.contains("rhythm")
                || normalized.contains("anger") || normalized.contains("dragon") || normalized.contains("blasphemous")) {
            return XENAGOS_COLORS.toArray(String[]::new);
        }
        if (normalized.contains("k'rrik") || normalized.contains("yawgmoth") || normalized.contains("phyrexian")
                || normalized.contains("swamp") || normalized.contains("black") || normalized.contains("blood")
                || normalized.contains("necrotic") || normalized.contains("demonic") || normalized.contains("vampiric")) {
            return KRRRIK_COLORS.toArray(String[]::new);
        }
        if (normalized.contains("azorius") || normalized.contains("teferi") || normalized.contains("narset")
                || normalized.contains("counter") || normalized.contains("swan") || normalized.contains("hanna")) {
            return GRAND_ARBITER_COLORS.toArray(String[]::new);
        }
        return new String[0];
    }

    private Set<String> basicLands() {
        return Set.of("plains", "island", "swamp", "mountain", "forest", "wastes");
    }

    private CardResponseDTO card(String name, String manaCost, String typeLine, String oracle, Double cmc, String... colorIdentity) {
        return new CardResponseDTO(name, manaCost, typeLine, oracle, cmc, List.of(colorIdentity), List.of());
    }

    private String fixture(String path) {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing fixture: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read fixture: " + path, exception);
        }
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public static class RecommendationFixtureProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.hibernate-orm.sql-load-script", "deck-recommendation-fixtures.sql");
        }
    }
}
