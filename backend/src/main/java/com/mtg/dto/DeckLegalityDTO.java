package com.mtg.dto;

import java.util.List;

public record DeckLegalityDTO(
        Long deckId,
        int mainDeckSize,
        int targetMainDeckSize,
        boolean sizeLegal,
        boolean singletonLegal,
        List<String> duplicateCards,
        List<String> colorIdentity,
        boolean colorIdentityLegal,
        List<String> offColorCards,
        BanlistStatusDTO banlist,
        boolean commanderValid,
        List<CommanderDTO> commanders,
        CompanionStatusDTO companion,
        BracketEstimateDTO estimatedBracket,
        boolean legal
) {
    public DeckLegalityDTO {
        duplicateCards = duplicateCards == null ? List.of() : List.copyOf(duplicateCards);
        colorIdentity = colorIdentity == null ? List.of() : List.copyOf(colorIdentity);
        offColorCards = offColorCards == null ? List.of() : List.copyOf(offColorCards);
        commanders = commanders == null ? List.of() : List.copyOf(commanders);
    }
}
