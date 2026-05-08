package com.mtg.controller;

import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.DeckImportDTO;
import com.mtg.service.CardService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
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

    @InjectMock
    CardService cardService;

    @BeforeEach
    void setupCards() {
        Mockito.when(cardService.normalizeLookupName(anyString())).thenAnswer(invocation -> normalize(invocation.getArgument(0)));
        Mockito.when(cardService.findByNames(any())).thenAnswer(invocation -> resolvedCards(invocation.getArgument(0)));
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
                .body("$", hasSize(greaterThanOrEqualTo(3)))
                .body("add", everyItem(not(is("Grand Arbiter Augustin IV"))))
                .body("remove", everyItem(not(is("Grand Arbiter Augustin IV"))))
                .body("reasoning", everyItem(not(is(""))))
                .body("budget", everyItem(nullValue()));
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
                .body("$", hasSize(greaterThanOrEqualTo(3)))
                .body("add", everyItem(not(is("Xenagos, God of Revels"))))
                .body("remove", everyItem(not(is("Xenagos, God of Revels"))))
                .body("reasoning", everyItem(not(is(""))))
                .body("budget", everyItem(nullValue()));
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

    private CardResponseDTO cardFor(String name) {
        String normalized = normalize(name);
        return switch (normalized) {
            case "grand arbiter augustin iv" -> card(name, "{2}{W}{U}", "Legendary Creature - Human Advisor", "White spells you cast cost {1} less to cast. Blue spells you cast cost {1} less to cast. Spells your opponents cast cost {1} more to cast.", 4.0, "W", "U");
            case "xenagos, god of revels" -> card(name, "{3}{R}{G}", "Legendary Enchantment Creature - God", "At the beginning of combat on your turn, another target creature you control gains haste and trample and gets +X/+X until end of turn.", 5.0, "R", "G");
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
            case "overwhelming stampede" -> card(name, "{3}{G}{G}", "Sorcery", "Creatures you control gain trample and get +X/+X until end of turn.", 5.0, "G");
            case "craterhoof behemoth" -> card(name, "{5}{G}{G}{G}", "Creature - Beast", "When this creature enters, creatures you control gain trample and get +X/+X until end of turn.", 8.0, "G");
            case "approach of the second sun" -> card(name, "{6}{W}", "Sorcery", "If this spell was cast from your hand and you've cast another spell named Approach of the Second Sun this game, you win the game.", 7.0, "W");
            case "shark typhoon" -> card(name, "{5}{U}", "Enchantment", "Whenever you cast a noncreature spell, create an X/X blue Shark creature token with flying.", 6.0, "U");
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
