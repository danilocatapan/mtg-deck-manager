package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.dto.DeckLegalityDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import com.mtg.service.rules.CommanderBanlistService;
import com.mtg.service.rules.CommanderBracketService;
import com.mtg.service.rules.CommanderGameChangerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeckLegalityRegressionTest {

    private static final String OWNER_ID = "google-user-1";

    @Mock
    DeckRepository deckRepository;

    @Mock
    CardService cardService;

    DeckLegalityService service;

    @BeforeEach
    void setup() {
        service = new DeckLegalityService();
        service.deckRepository = deckRepository;
        service.cardService = cardService;
        service.commanderBanlistService = new CommanderBanlistService();
        service.commanderGameChangerService = new CommanderGameChangerService();
        service.comboDetectionService = new ComboDetectionService();
        service.commanderBracketService = new CommanderBracketService();
        lenient().when(cardService.normalizeLookupName(anyString())).thenAnswer(invocation -> normalize(invocation.getArgument(0)));
        lenient().when(cardService.findByNames(any())).thenAnswer(invocation -> resolvedCards(invocation.getArgument(0)));
    }

    @Test
    void preconLikeDeckStaysCasual() {
        DeckLegalityDTO legality = check(deck("Cmd", "G", List.of(
                card("Forest", 98),
                card("Sol Ring", 1)
        )));

        assertTrue(legality.legal());
        assertEquals(0, legality.gameChangerCount());
        assertTrue(legality.estimatedBracket().level() <= 2);
    }

    @Test
    void upgradedDeckWithGameChangersRaisesToBracketThree() {
        DeckLegalityDTO legality = check(deck("Blue Cmd", "U", List.of(
                card("Island", 96),
                card("Rhystic Study", 1),
                card("Cyclonic Rift", 1),
                card("Sol Ring", 1)
        )));

        assertTrue(legality.legal());
        assertEquals(2, legality.gameChangerCount());
        assertEquals(3, legality.estimatedBracket().level());
    }

    @Test
    void highPowerDeckWithFourGameChangersRaisesToBracketFour() {
        DeckLegalityDTO legality = check(deck("Five Color Cmd", "WUBRG", List.of(
                card("Forest", 95),
                card("Rhystic Study", 1),
                card("Cyclonic Rift", 1),
                card("Demonic Tutor", 1),
                card("Smothering Tithe", 1)
        )));

        assertTrue(legality.legal());
        assertEquals(4, legality.gameChangerCount());
        assertEquals(4, legality.estimatedBracket().level());
    }

    @Test
    void cedhSignalsRaiseToBracketFive() {
        DeckLegalityDTO legality = check(deck("Five Color Cmd", "WUBRG", List.of(
                card("Forest", 90),
                card("Thassa's Oracle", 1),
                card("Demonic Consultation", 1),
                card("Demonic Tutor", 1),
                card("Vampiric Tutor", 1),
                card("Mystical Tutor", 1),
                card("Mana Vault", 1),
                card("Chrome Mox", 1),
                card("Mox Diamond", 1),
                card("Force of Will", 1)
        )));

        assertTrue(legality.legal());
        assertEquals(5, legality.estimatedBracket().level());
    }

    @Test
    void partnerBackgroundContractRemainsLegal() {
        Deck deck = deck("Wilson, Refined Grizzly", "G", List.of(
                card("Forest", 98),
                card("Sol Ring", 1)
        ));
        deck.setCommandersJson("[{\"name\":\"Wilson, Refined Grizzly\",\"role\":\"commander\"},{\"name\":\"Raised by Giants\",\"role\":\"background\"}]");

        DeckLegalityDTO legality = check(deck);

        assertTrue(legality.legal());
        assertEquals(2, legality.commanders().size());
    }

    @Test
    void colorlessDeckWithWastesIsLegal() {
        DeckLegalityDTO legality = check(deck("Kozilek, the Great Distortion", "", List.of(
                card("Wastes", 98),
                card("Sol Ring", 1)
        )));

        assertTrue(legality.legal());
        assertTrue(legality.colorIdentity().isEmpty());
    }

    @Test
    void fiveColorDeckAllowsAllColors() {
        DeckLegalityDTO legality = check(deck("Five Color Cmd", "WUBRG", List.of(
                card("Forest", 95),
                card("Swords to Plowshares", 1),
                card("Counterspell", 1),
                card("Demonic Tutor", 1),
                card("Lightning Bolt", 1)
        )));

        assertTrue(legality.legal());
        assertTrue(legality.colorIdentityLegal());
    }

    @Test
    void offColorCardMakesDeckIllegalAndExhibition() {
        DeckLegalityDTO legality = check(deck("Cmd", "G", List.of(
                card("Forest", 98),
                card("Swords to Plowshares", 1)
        )));

        assertFalse(legality.legal());
        assertFalse(legality.colorIdentityLegal());
        assertEquals(1, legality.estimatedBracket().level());
    }

    private DeckLegalityDTO check(Deck deck) {
        deck.setId(1L);
        deck.setOwnerId(OWNER_ID);
        when(deckRepository.findByIdAndOwner(1L, OWNER_ID)).thenReturn(deck);
        return service.check(1L, OWNER_ID);
    }

    private Deck deck(String commander, String colorIdentity, List<DeckCard> cards) {
        Deck deck = new Deck();
        deck.setName("Regression Deck");
        deck.setCommander(commander);
        deck.setColorIdentity(colorIdentity);
        deck.setCards(new ArrayList<>(cards));
        return deck;
    }

    private DeckCard card(String name, int quantity) {
        return new DeckCard(name, quantity);
    }

    private Map<String, CardResponseDTO> resolvedCards(List<String> names) {
        Map<String, CardResponseDTO> cards = new LinkedHashMap<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            cards.put(normalize(name), cardFor(name));
        }
        return cards;
    }

    private CardResponseDTO cardFor(String name) {
        String normalized = normalize(name);
        return switch (normalized) {
            case "cmd" -> card(name, "{2}{G}", "Legendary Creature - Elf", "", 3.0, "G");
            case "blue cmd" -> card(name, "{2}{U}", "Legendary Creature - Wizard", "", 3.0, "U");
            case "five color cmd" -> card(name, "{W}{U}{B}{R}{G}", "Legendary Creature - Avatar", "", 5.0, "W", "U", "B", "R", "G");
            case "wilson, refined grizzly" -> card(name, "{1}{G}", "Legendary Creature - Bear Warrior", "Choose a Background.", 2.0, "G");
            case "raised by giants" -> card(name, "{5}{G}", "Legendary Enchantment - Background", "Commander creatures you own have base power and toughness 10/10.", 6.0, "G");
            case "kozilek, the great distortion" -> card(name, "{8}{C}{C}", "Legendary Creature - Eldrazi", "", 10.0);
            case "forest" -> card(name, "", "Basic Land - Forest", "Add {G}.", 0.0, "G");
            case "island" -> card(name, "", "Basic Land - Island", "Add {U}.", 0.0, "U");
            case "wastes" -> card(name, "", "Basic Land", "Add {C}.", 0.0);
            case "sol ring" -> card(name, "{1}", "Artifact", "{T}: Add {C}{C}.", 1.0);
            case "rhystic study" -> card(name, "{2}{U}", "Enchantment", "Whenever an opponent casts a spell, you may draw a card unless that player pays {1}.", 3.0, "U");
            case "cyclonic rift" -> card(name, "{1}{U}", "Instant", "Return target nonland permanent you don't control to its owner's hand.", 2.0, "U");
            case "demonic tutor" -> card(name, "{1}{B}", "Sorcery", "Search your library for a card, put that card into your hand, then shuffle.", 2.0, "B");
            case "smothering tithe" -> card(name, "{3}{W}", "Enchantment", "Whenever an opponent draws a card, that player may pay {2}. If they don't, you create a Treasure token.", 4.0, "W");
            case "thassa's oracle" -> card(name, "{U}{U}", "Creature - Merfolk Wizard", "When this creature enters, look at the top X cards. If X is greater than or equal to the number of cards in your library, you win the game.", 2.0, "U");
            case "demonic consultation" -> card(name, "{B}", "Instant", "Name a card. Exile the top six cards of your library, then reveal cards until you reveal the named card.", 1.0, "B");
            case "vampiric tutor" -> card(name, "{B}", "Instant", "Search your library for a card, then shuffle and put that card on top.", 1.0, "B");
            case "mystical tutor" -> card(name, "{U}", "Instant", "Search your library for an instant or sorcery card, reveal it, then shuffle and put it on top.", 1.0, "U");
            case "mana vault" -> card(name, "{1}", "Artifact", "{T}: Add {C}{C}{C}.", 1.0);
            case "chrome mox" -> card(name, "{0}", "Artifact", "Imprint. {T}: Add one mana of any of the exiled card's colors.", 0.0);
            case "mox diamond" -> card(name, "{0}", "Artifact", "If Mox Diamond would enter, discard a land card instead. {T}: Add one mana of any color.", 0.0);
            case "force of will" -> card(name, "{3}{U}{U}", "Instant", "You may pay 1 life and exile a blue card rather than pay this spell's mana cost. Counter target spell.", 5.0, "U");
            case "jegantha, the wellspring" -> card(name, "{4}{G/R}", "Legendary Creature - Elemental Elk", "Companion. {T}: Add {W}{U}{B}{R}{G}.", 5.0, "W", "U", "B", "R", "G");
            case "swords to plowshares" -> card(name, "{W}", "Instant", "Exile target creature.", 1.0, "W");
            case "counterspell" -> card(name, "{U}{U}", "Instant", "Counter target spell.", 2.0, "U");
            case "lightning bolt" -> card(name, "{R}", "Instant", "Deal 3 damage to any target.", 1.0, "R");
            default -> card(name, "", "", "", 0.0);
        };
    }

    private CardResponseDTO card(String name, String manaCost, String typeLine, String oracle, Double cmc, String... colors) {
        return new CardResponseDTO(name.trim(), manaCost, typeLine, oracle, cmc, List.of(colors), List.of());
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
