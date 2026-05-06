package com.mtg.service.meta;

import com.mtg.dto.CardResponseDTO;
import com.mtg.service.CardService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class CardDataEnricher {

    @Inject
    CardService cardService;

    public List<MetaDeckCard> enrich(List<MetaDeckCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return List.of();
        }
        Map<String, CardResponseDTO> knownCards = cardService.findByNames(cards.stream().map(MetaDeckCard::name).toList());
        return cards.stream()
                .map(card -> enrich(card, knownCards.get(normalize(card.name()))))
                .toList();
    }

    private MetaDeckCard enrich(MetaDeckCard metaCard, CardResponseDTO card) {
        if (card == null) {
            return metaCard;
        }
        return new MetaDeckCard(
                metaCard.name(),
                metaCard.quantity(),
                metaCard.roles(),
                card.cmc(),
                card.typeLine(),
                card.colorIdentity()
        );
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
