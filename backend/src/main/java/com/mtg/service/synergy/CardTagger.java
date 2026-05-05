package com.mtg.service.synergy;

import com.mtg.dto.CardResponseDTO;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class CardTagger {

    public Set<String> tagCard(CardResponseDTO card) {
        Set<String> tags = new HashSet<>();
        if (card == null) return tags;
        String text = card.oracleText() != null ? card.oracleText().toLowerCase() : "";
        String type = card.typeLine() != null ? card.typeLine().toLowerCase() : "";

        if (text.contains("draw")) tags.add("draw");
        if (text.contains("draw a card")) tags.add("draw");
        if (text.contains("create") || text.contains("token")) tags.add("token");
        if (text.contains("sacrifice") || text.contains("sacrifice a")) tags.add("sacrifice");
        if (text.contains("graveyard") || text.contains("mill") || text.contains("recur")) tags.add("graveyard");
        if (text.contains("haste")) tags.add("haste");
        if (text.contains("trample")) tags.add("trample");
        if (text.contains("double strike")) tags.add("double-strike");
        if (text.contains("combat")) tags.add("combat");
        if (text.contains("add {") || text.contains("add ")) tags.add("ramp");
        if (type.contains("creature") && (text.contains("power") || text.contains("toughness"))) tags.add("big-creature");

        return tags;
    }
}
