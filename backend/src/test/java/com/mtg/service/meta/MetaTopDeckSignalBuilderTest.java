package com.mtg.service.meta;

import com.mtg.dto.CardResponseDTO;
import com.mtg.model.MetaDeckArchetype;
import com.mtg.model.MetaDeckBracket;
import com.mtg.model.MetaDeckCardSection;
import com.mtg.model.MetaDeckFormat;
import com.mtg.model.MetaDeckSource;
import com.mtg.model.MetaRankingPeriod;
import com.mtg.model.MetaTopDeck;
import com.mtg.model.MetaTopDeckCard;
import com.mtg.service.CardService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class MetaTopDeckSignalBuilderTest {

    @Test
    void buildsRecommendationProfilesOnlyWhenSampleIsUseful() {
        MetaTopDeckSignalBuilder builder = new MetaTopDeckSignalBuilder();
        builder.cardService = cardService();

        List<CommanderMetaProfile> profiles = builder.buildProfiles(List.of(
                deck(1, "BRACKET_5", "Thassa's Oracle", "Demonic Consultation"),
                deck(2, "BRACKET_5", "Thassa's Oracle", "Demonic Consultation"),
                deck(3, "BRACKET_5", "Thassa's Oracle", "Mystic Remora")
        ));

        assertEquals(1, profiles.size());
        CommanderMetaProfile profile = profiles.getFirst();
        assertEquals("Talion, the Kindly Lord", profile.commander());
        assertEquals("cedh", profile.bracket());
        assertEquals(3, profile.sampleSize());
        assertEquals(List.of("meta_top_decks"), profile.sourcesUsed());
        assertEquals("Thassa's Oracle", profile.topCards().getFirst().getName());
        assertEquals(1.0, profile.topCards().getFirst().getInclusion(), 0.001);
        assertTrue(profile.topCards().stream().allMatch(card -> "meta_top_decks".equals(card.getSource())));
    }

    @Test
    void keepsInsufficientSamplesInactive() {
        MetaTopDeckSignalBuilder builder = new MetaTopDeckSignalBuilder();
        builder.cardService = cardService();

        List<CommanderMetaProfile> profiles = builder.buildProfiles(List.of(
                deck(1, "BRACKET_5", "Thassa's Oracle"),
                deck(2, "BRACKET_5", "Thassa's Oracle")
        ));

        assertEquals(0, profiles.size());
    }

    private MetaTopDeck deck(int rank, String bracket, String... mainCards) {
        MetaTopDeck deck = new MetaTopDeck();
        deck.setSource(MetaDeckSource.MANUAL);
        deck.setName("Talion Top " + rank);
        deck.setFormat(MetaDeckFormat.COMMANDER);
        deck.setCommander("Talion, the Kindly Lord");
        deck.setCommanderNormalized("talion, the kindly lord");
        deck.setRank(rank);
        deck.setRankingPeriod(MetaRankingPeriod.MONTHLY);
        deck.setRankingDate(LocalDate.now().minusDays(rank));
        deck.setArchetype(MetaDeckArchetype.CONTROL);
        deck.setBracket(MetaDeckBracket.valueOf(bracket));
        deck.setCreatedAt(OffsetDateTime.now());
        deck.setUpdatedAt(OffsetDateTime.now());
        java.util.ArrayList<MetaTopDeckCard> cards = new java.util.ArrayList<>();
        cards.add(card("Talion, the Kindly Lord", MetaDeckCardSection.COMMANDER));
        for (String mainCard : mainCards) {
            cards.add(card(mainCard, MetaDeckCardSection.MAIN));
        }
        deck.setCards(cards);
        return deck;
    }

    private MetaTopDeckCard card(String name, MetaDeckCardSection section) {
        MetaTopDeckCard card = new MetaTopDeckCard();
        card.setName(name);
        card.setNameNormalized(name.toLowerCase());
        card.setQuantity(1);
        card.setSection(section);
        card.setCreatedAt(OffsetDateTime.now());
        return card;
    }

    private CardService cardService() {
        CardService service = Mockito.mock(CardService.class);
        when(service.normalizeLookupName(Mockito.anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class).toLowerCase());
        when(service.findByNames(Mockito.anyList())).thenAnswer(invocation -> {
            List<String> names = invocation.getArgument(0);
            Map<String, CardResponseDTO> cards = new LinkedHashMap<>();
            for (String name : names) {
                cards.put(name.toLowerCase(), new CardResponseDTO(name, "", "Instant", "", 1.0, List.of("U"), List.of()));
            }
            return cards;
        });
        return service;
    }
}
