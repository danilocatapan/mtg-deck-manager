package com.mtg.service;

import com.mtg.dto.CardResponseDTO;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ColorIdentityMatcher {

    private ColorIdentityMatcher() {}

    public static Set<String> normalize(List<String> colorIdentity) {
        if (colorIdentity == null) return Set.of();
        return colorIdentity.stream().map(String::trim).collect(Collectors.toSet());
    }

    public static boolean matches(CardResponseDTO card, Set<String> commanderColors) {
        if (card == null) return false;
        List<String> ci = card.colorIdentity();
        if (ci == null || ci.isEmpty()) return true; // colorless cards allowed
        Set<String> cardColors = normalize(ci);
        if (commanderColors == null || commanderColors.isEmpty()) return true;
        return commanderColors.containsAll(cardColors);
    }
}
