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
        // normalize once: lowercase and remove punctuation to avoid inconsistent contains checks
        String raw = card.oracleText() != null ? card.oracleText() : "";
        String text = raw.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        String type = card.typeLine() != null ? card.typeLine().toLowerCase().split("\\s+//\\s+", 2)[0] : "";

        if (text.contains("draw")) tags.add("draw");
        if (text.contains("look at the top") || text.contains("exile the top") || text.contains("play that card") || text.contains("cast that card")) tags.add("impulse-draw");
        if (text.contains("create") || text.contains("token")) tags.add("token");
        if (text.contains("treasure")) tags.add("treasure");
        if (text.contains("sacrifice")) tags.add("sacrifice");
        if (text.contains("sacrifice a creature") || text.contains("sacrifice another") || text.contains("sacrifice an artifact")) tags.add("sacrifice-outlet");
        if (text.contains("graveyard") || text.contains("mill") || text.contains("recur")) tags.add("graveyard");
        if (text.contains("return target") && text.contains("graveyard")) tags.add("recursion");
        if (text.contains("mill")) tags.add("self-mill");
        if (text.contains("haste")) tags.add("haste");
        if (text.contains("trample")) tags.add("trample");
        if (text.contains("double strike") || text.contains("doublestrike")) tags.add("double-strike");
        if (text.contains("combat")) tags.add("combat");
        if (text.contains("additional combat") || text.contains("extra combat") || text.contains("untap all attacking creatures")) tags.add("combo-piece");
        if (text.contains("infect")) tags.add("infect");
        if (!type.contains("land") && (text.contains("add ") || text.contains("addc"))) tags.add("ramp");
        if (text.contains("add one mana of any color") || text.contains("any color") || text.contains("mana of any one color")) tags.add("fixing");
        if (type.contains("artifact") && (text.contains("add ") || text.contains("mana"))) tags.add("mana-rock");
        if (type.contains("land") && (text.contains("search your library") || text.contains("sacrifice") && text.contains("basic land"))) tags.add("fetch-land");
        if (text.contains("counter target")) tags.add("stack-interaction");
        if (text.contains("destroy") || text.contains("exile target") || text.contains("return target")) tags.add("removal");
        if (text.contains("hexproof") || text.contains("indestructible") || text.contains("phase out") || text.contains("protection")) tags.add("protection");
        if (type.contains("creature") && (text.contains("power") || text.contains("toughness"))) tags.add("big-creature");

        return tags;
    }
}
