package com.mtg.service;

import com.mtg.dto.DeckCardDTO;
import com.mtg.dto.CardLookupRequestDTO;
import com.mtg.dto.DeckRequestDTO;
import com.mtg.dto.DeckResponseDTO;
import com.mtg.dto.ApplyRecommendationSwapDTO;
import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.repository.DeckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    private static final String OWNER_ID = "google-user-1";

    @Mock
    DeckRepository deckRepository;

    @Mock
    DeckImportService importService;

    @Mock
    CardService cardService;

    @InjectMocks
    DeckService deckService;

    @BeforeEach
    void setup() {
        deckService.importService = importService;
        deckService.cardService = cardService;
        lenient().when(cardService.normalizeLookupName(anyString())).thenAnswer(invocation -> normalize(invocation.getArgument(0)));
        lenient().when(cardService.findByNames(any())).thenAnswer(invocation -> resolvedCards(invocation.getArgument(0)));
        lenient().when(cardService.findByCardRequests(any())).thenAnswer(invocation -> resolvedLookupCards(invocation.getArgument(0)));
    }

    @Test
    void createDeck_success() {
        DeckRequestDTO request = new DeckRequestDTO("My Deck", "Commander", List.of(new DeckCardDTO("Sol Ring",1)));
        doAnswer(invocation -> {
            Deck d = invocation.getArgument(0);
            d.setId(1L);
            return null;
        }).when(deckRepository).persist(any(Deck.class));

        DeckResponseDTO resp = deckService.createDeck(request, OWNER_ID);

        assertNotNull(resp.id());
        assertEquals("My Deck", resp.name());
        assertEquals("R", resp.colorIdentity());
        assertEquals(1, resp.commanders().size());
        assertEquals("Commander", resp.commanders().getFirst().name());
        verify(deckRepository).persist(org.mockito.ArgumentMatchers.<Deck>argThat(deck -> OWNER_ID.equals(deck.getOwnerId())));
    }

    @Test
    void createDeck_validationFails() {
        DeckRequestDTO request = new DeckRequestDTO(" ", "Commander", List.of());
        assertThrows(IllegalArgumentException.class, () -> deckService.createDeck(request, OWNER_ID));
    }

    @Test
    void createDeck_failsWhenCommanderOrCardDoesNotExist() {
        DeckRequestDTO request = new DeckRequestDTO("My Deck", "Commander", List.of(new DeckCardDTO("Missing Card", 1)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> deckService.createDeck(request, OWNER_ID)
        );

        assertEquals("Card not found: Missing Card", exception.getMessage());
        verify(deckRepository, never()).persist(any(Deck.class));
    }

    @Test
    void updateDeck_notFound() {
        Long id = 999L;
        when(deckRepository.findByIdAndOwner(id, OWNER_ID)).thenReturn(null);
        DeckRequestDTO request = new DeckRequestDTO("Name","Cmd", List.of(new DeckCardDTO("Sol Ring",1)));
        assertNull(deckService.updateDeck(id, request, OWNER_ID));
    }

    @Test
    void exportDeck_withCards() {
        Deck deck = new Deck("My Deck", "Cmd", List.of(new DeckCard("Sol Ring",1), new DeckCard("Command Tower",1)));
        when(deckRepository.findByIdAndOwner(1L, OWNER_ID)).thenReturn(deck);

        String exported = deckService.exportDeck(1L, OWNER_ID);

        assertEquals("1 Sol Ring\n1 Command Tower", exported);
    }

    @Test
    void exportDeck_includesAllDeckCards() {
        Deck deck = new Deck("My Deck", "Cmd", List.of(
                new DeckCard("Sol Ring", 1),
                new DeckCard("Beast Within", 1)
        ));
        when(deckRepository.findByIdAndOwner(1L, OWNER_ID)).thenReturn(deck);

        String exported = deckService.exportDeck(1L, OWNER_ID);

        assertEquals("1 Sol Ring\n1 Beast Within", exported);
    }

    @Test
    void exportDeck_empty() {
        Deck deck = new Deck("Empty", "Cmd", List.of());
        when(deckRepository.findByIdAndOwner(2L, OWNER_ID)).thenReturn(deck);

        String exported = deckService.exportDeck(2L, OWNER_ID);

        assertEquals("", exported);
    }

    @Test
    void exportDeck_notFound() {
        when(deckRepository.findByIdAndOwner(999L, OWNER_ID)).thenReturn(null);
        assertNull(deckService.exportDeck(999L, OWNER_ID));
    }

    @Test
    void importDeck_reportsActualCardCountWhenTooLarge() {
        when(importService.parse(any(), any())).thenReturn(List.of(
                new DeckCard("Mountain", 60),
                new DeckCard("Forest", 45)
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> deckService.importDeck(new com.mtg.dto.DeckImportDTO("Big Deck", "Cmd", "60 Mountain\n45 Forest"), OWNER_ID)
        );

        assertEquals("Imported deck has 105 cards; maximum is 99.", exception.getMessage());
    }

    @Test
    void importDeck_preservesPrintingMetadataFromParsedCards() {
        when(importService.parse(any(), any())).thenReturn(List.of(
                new DeckCard("Winota, Joiner of Forces", 1, "IKO", "349", "FOIL")
        ));
        doAnswer(invocation -> {
            Deck deck = invocation.getArgument(0);
            deck.setId(10L);
            return null;
        }).when(deckRepository).persist(any(Deck.class));

        DeckResponseDTO response = deckService.importDeck(
                new com.mtg.dto.DeckImportDTO("Winota Deck", "Commander", "1 Winota, Joiner of Forces (IKO) 349 *F*", null, null, "MOXFIELD"),
                OWNER_ID
        );

        DeckCardDTO importedCard = response.cards().getFirst();
        assertEquals("IKO", importedCard.setCode());
        assertEquals("349", importedCard.collectorNumber());
        assertEquals("FOIL", importedCard.finish());
        assertEquals("scryfall-winota, joiner of forces", importedCard.scryfallId());
        assertEquals("https://img.test/winota, joiner of forces.jpg", importedCard.imageUrl());
    }

    @Test
    void importDeck_removesCommanderFromMainDeckWhenItAppearsInImportedList() {
        when(importService.parse(any(), any())).thenReturn(List.of(
                new DeckCard("Any Commander", 1),
                new DeckCard("Forest", 99)
        ));
        doAnswer(invocation -> {
            Deck deck = invocation.getArgument(0);
            deck.setId(11L);
            return null;
        }).when(deckRepository).persist(any(Deck.class));

        DeckResponseDTO response = deckService.importDeck(
                new com.mtg.dto.DeckImportDTO("Commander In List", "Any Commander", "1 Any Commander\n99 Forest"),
                OWNER_ID
        );

        assertEquals(99, response.cards().stream().mapToInt(DeckCardDTO::quantity).sum());
        assertTrue(response.cards().stream().noneMatch(card -> normalize(card.name()).equals("any commander")));
        assertEquals("Any Commander", response.commander());
    }

    @Test
    void createDeck_requiresOwner() {
        DeckRequestDTO request = new DeckRequestDTO("My Deck", "Commander", List.of(new DeckCardDTO("Sol Ring",1)));

        assertThrows(IllegalArgumentException.class, () -> deckService.createDeck(request, " "));
        verify(deckRepository, never()).persist(any(Deck.class));
    }

    @Test
    void createDeck_usesSimpleDeckCardsWithoutZones() {
        DeckRequestDTO request = new DeckRequestDTO("My Deck", "Commander", List.of(
                new DeckCardDTO("Sol Ring", 1),
                new DeckCardDTO("Beast Within", 1)
        ));
        doAnswer(invocation -> {
            Deck deck = invocation.getArgument(0);
            deck.setId(1L);
            return null;
        }).when(deckRepository).persist(any(Deck.class));

        DeckResponseDTO response = deckService.createDeck(request, OWNER_ID);

        assertTrue(response.cards().stream().anyMatch(card -> card.name().equals("Sol Ring")));
        assertTrue(response.cards().stream().anyMatch(card -> card.name().equals("Beast Within")));
    }

    @Test
    void applyRecommendationSwap_validSwap() {
        Deck deck = new Deck("My Deck", "Cmd", List.of(
                new DeckCard("Naturalize", 1),
                new DeckCard("Sol Ring", 1)
        ));
        deck.setId(1L);
        deck.setOwnerId(OWNER_ID);
        when(deckRepository.findByIdAndOwner(1L, OWNER_ID)).thenReturn(deck);

        DeckResponseDTO response = deckService.applyRecommendationSwap(
                1L,
                new ApplyRecommendationSwapDTO("Beast Within", "Naturalize"),
                OWNER_ID
        );

        assertEquals(2, response.cards().stream().mapToInt(DeckCardDTO::quantity).sum());
        assertTrue(response.cards().stream().anyMatch(card -> card.name().equals("Beast Within") && card.quantity() == 1));
        assertTrue(response.cards().stream().noneMatch(card -> card.name().equals("Naturalize")));
        assertEquals(1, response.history().size());
        assertEquals("Beast Within", response.history().getFirst().add());
        assertFalse(response.history().getFirst().undone());
        verify(deckRepository).persist(deck);
    }

    @Test
    void undoRecommendationSwap_revertsLatestActiveSwap() {
        Deck deck = new Deck("My Deck", "Cmd", List.of(
                new DeckCard("Naturalize", 1),
                new DeckCard("Sol Ring", 1)
        ));
        deck.setId(1L);
        deck.setOwnerId(OWNER_ID);
        when(deckRepository.findByIdAndOwner(1L, OWNER_ID)).thenReturn(deck);

        DeckResponseDTO applied = deckService.applyRecommendationSwap(
                1L,
                new ApplyRecommendationSwapDTO("Beast Within", "Naturalize", "swap-1", "meta_profile", "medium", "Pouca interacao.", "Risco baixo.", "Interacao +1"),
                OWNER_ID
        );

        DeckResponseDTO undone = deckService.undoRecommendationSwap(1L, applied.history().getFirst().id(), OWNER_ID);

        assertTrue(undone.cards().stream().anyMatch(card -> card.name().equals("Naturalize")));
        assertTrue(undone.cards().stream().noneMatch(card -> card.name().equals("Beast Within")));
        assertTrue(undone.history().getFirst().undone());
        verify(deckRepository, times(2)).persist(deck);
    }

    @Test
    void applyRecommendationSwap_returnsNullWhenDeckDoesNotExist() {
        when(deckRepository.findByIdAndOwner(999L, OWNER_ID)).thenReturn(null);

        DeckResponseDTO response = deckService.applyRecommendationSwap(
                999L,
                new ApplyRecommendationSwapDTO("Beast Within", "Naturalize"),
                OWNER_ID
        );

        assertNull(response);
        verify(deckRepository, never()).persist(any(Deck.class));
    }

    @Test
    void applyRecommendationSwap_failsWhenRemoveDoesNotExist() {
        Deck deck = new Deck("My Deck", "Cmd", List.of(new DeckCard("Sol Ring", 1)));
        when(deckRepository.findByIdAndOwner(1L, OWNER_ID)).thenReturn(deck);

        assertThrows(
                IllegalArgumentException.class,
                () -> deckService.applyRecommendationSwap(1L, new ApplyRecommendationSwapDTO("Beast Within", "Naturalize"), OWNER_ID)
        );
    }

    @Test
    void applyRecommendationSwap_failsWhenAddDoesNotExist() {
        Deck deck = new Deck("My Deck", "Cmd", List.of(new DeckCard("Naturalize", 1)));
        when(deckRepository.findByIdAndOwner(1L, OWNER_ID)).thenReturn(deck);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> deckService.applyRecommendationSwap(1L, new ApplyRecommendationSwapDTO("Missing Card", "Naturalize"), OWNER_ID)
        );

        assertEquals("Card to add was not found: Missing Card", exception.getMessage());
        verify(deckRepository, never()).persist(deck);
    }

    @Test
    void applyRecommendationSwap_failsWhenAddAlreadyExists() {
        Deck deck = new Deck("My Deck", "Cmd", List.of(
                new DeckCard("Beast Within", 1),
                new DeckCard("Naturalize", 1)
        ));
        when(deckRepository.findByIdAndOwner(1L, OWNER_ID)).thenReturn(deck);

        assertThrows(
                IllegalArgumentException.class,
                () -> deckService.applyRecommendationSwap(1L, new ApplyRecommendationSwapDTO("Beast Within", "Naturalize"), OWNER_ID)
        );
    }

    @Test
    void applyRecommendationSwap_allowsExistingBasicLand() {
        Deck deck = new Deck("My Deck", "Cmd", List.of(
                new DeckCard("Forest", 8),
                new DeckCard("Naturalize", 1)
        ));
        when(deckRepository.findByIdAndOwner(1L, OWNER_ID)).thenReturn(deck);

        DeckResponseDTO response = deckService.applyRecommendationSwap(
                1L,
                new ApplyRecommendationSwapDTO("Forest", "Naturalize"),
                OWNER_ID
        );

        assertTrue(response.cards().stream().anyMatch(card -> card.name().equals("Forest") && card.quantity() == 9));
        assertEquals(9, response.cards().stream().mapToInt(DeckCardDTO::quantity).sum());
    }

    @Test
    void applyRecommendationSwap_failsWhenPayloadIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> deckService.applyRecommendationSwap(1L, null, OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> deckService.applyRecommendationSwap(1L, new ApplyRecommendationSwapDTO(" ", "Naturalize"), OWNER_ID));
        assertThrows(IllegalArgumentException.class, () -> deckService.applyRecommendationSwap(1L, new ApplyRecommendationSwapDTO("Naturalize", "naturalize"), OWNER_ID));
    }

    @Test
    void applyRecommendationSwap_failsWhenDeckAlreadyExceeds99Cards() {
        Deck deck = new Deck("My Deck", "Cmd", List.of(
                new DeckCard("Forest", 99),
                new DeckCard("Naturalize", 1)
        ));
        when(deckRepository.findByIdAndOwner(1L, OWNER_ID)).thenReturn(deck);

        assertThrows(
                IllegalArgumentException.class,
                () -> deckService.applyRecommendationSwap(1L, new ApplyRecommendationSwapDTO("Beast Within", "Naturalize"), OWNER_ID)
        );
        verify(deckRepository, never()).persist(deck);
    }

    private Map<String, CardResponseDTO> resolvedCards(List<String> names) {
        Map<String, CardResponseDTO> cards = new LinkedHashMap<>();
        for (String name : names) {
            if (name == null || name.isBlank() || normalize(name).equals("missing card")) {
                continue;
            }
            cards.put(normalize(name), cardFor(name));
        }
        return cards;
    }

    private Map<String, CardResponseDTO> resolvedLookupCards(List<CardLookupRequestDTO> lookups) {
        Map<String, CardResponseDTO> cards = new LinkedHashMap<>();
        for (CardLookupRequestDTO lookup : lookups) {
            if (lookup == null || lookup.name() == null || normalize(lookup.name()).equals("missing card")) {
                continue;
            }
            CardResponseDTO base = cardFor(lookup.name());
            cards.put(lookup.lookupKey(), new CardResponseDTO(
                    base.name(),
                    base.manaCost(),
                    base.typeLine(),
                    base.oracleText(),
                    base.cmc(),
                    base.colorIdentity(),
                    base.keywords(),
                    "https://img.test/" + normalize(base.name()) + ".jpg",
                    base.estimatedPrice(),
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
        if (normalized.equals("commander") || normalized.equals("cmd")) {
            return new CardResponseDTO(name.trim(), "{2}{R}", "Legendary Creature - Warrior", "", 3.0, List.of("R"), List.of());
        }
        if (normalized.equals("forest")) {
            return new CardResponseDTO(name.trim(), "", "Basic Land - Forest", "Add {G}.", 0.0, List.of("G"), List.of());
        }
        return new CardResponseDTO(name.trim(), "", "", "", 0.0, List.of(), List.of());
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
