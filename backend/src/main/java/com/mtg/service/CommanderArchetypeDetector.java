package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.service.synergy.SynergyEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class CommanderArchetypeDetector {

    @Inject
    SynergyEngine synergyEngine;

    public CommanderArchetypeProfile detect(String commanderName, CardResponseDTO commanderCard, DeckRoleSummary roleSummary, Set<String> fallbackColors) {
        Set<String> colors = commanderCard != null && commanderCard.colorIdentity() != null
                ? new HashSet<>(commanderCard.colorIdentity())
                : new HashSet<>(fallbackColors);
        Set<String> commanderTags = commanderCard != null ? synergyEngine.tagsForCard(commanderCard) : Set.of();
        Set<String> allTags = new HashSet<>(roleSummary.deckTags());
        allTags.addAll(commanderTags);

        String archetype;
        String plan;
        if (allTags.contains("token")) {
            archetype = "tokens";
            plan = "criar massa de permanentes, converter quantidade em dano ou valor e manter pressão incremental";
        } else if (allTags.contains("sacrifice") || allTags.contains("graveyard")) {
            archetype = "aristocrats";
            plan = "gerar valor com sacrifícios, recorrência e recursos do cemitério";
        } else if (allTags.contains("combat") || allTags.contains("trample") || allTags.contains("big-creature")) {
            archetype = "combat damage";
            plan = "acelerar ameaças relevantes e transformar combate em pressão letal";
        } else if (roleSummary.removal() >= 10 && roleSummary.averageCmc() <= 3.4) {
            archetype = "control";
            plan = "controlar a mesa, trocar recursos com eficiência e vencer com poucas ameaças resilientes";
        } else if (roleSummary.ramp() >= 12) {
            archetype = "ramp";
            plan = "acelerar mana cedo e converter vantagem de recursos em ameaças de alto impacto";
        } else {
            archetype = "value";
            plan = "acumular vantagem incremental, manter consistência e vencer por qualidade média das cartas";
        }

        return new CommanderArchetypeProfile(commanderName, colors, archetype, plan, commanderTags);
    }
}
