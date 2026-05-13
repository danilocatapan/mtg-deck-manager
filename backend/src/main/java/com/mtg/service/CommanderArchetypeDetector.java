package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.service.synergy.SynergyEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class CommanderArchetypeDetector {
    private static final Logger LOG = Logger.getLogger(CommanderArchetypeDetector.class);

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
        if (allTags.contains("stax")) {
            archetype = "stax";
            plan = "limitar recursos dos oponentes e vencer com vantagem incremental protegida";
        } else if (allTags.contains("combo-piece") && (allTags.contains("tutor") || roleSummary.averageCmc() <= 2.8)) {
            archetype = "turbo-combo";
            plan = "montar uma condicao de vitoria compacta com velocidade e protecao";
        } else if (allTags.contains("token")) {
            archetype = "tokens";
            plan = "criar massa de permanentes, converter quantidade em dano ou valor e manter pressao incremental";
        } else if (allTags.contains("sacrifice") || allTags.contains("graveyard")) {
            archetype = "aristocrats";
            plan = "gerar valor com sacrificios, recorrencia e recursos do cemiterio";
        } else if (allTags.contains("recursion") || allTags.contains("self-mill")) {
            archetype = "reanimator";
            plan = "transformar cemiterio em recurso e reutilizar ameacas ou pecas-chave";
        } else if (allTags.contains("counterspell") && allTags.contains("selection")) {
            archetype = "spellslinger";
            plan = "encadear magicas baratas, selecao e interacao para manter ritmo";
        } else if (allTags.contains("combat") || allTags.contains("trample") || allTags.contains("big-creature")) {
            archetype = "combat";
            plan = "acelerar ameacas relevantes e transformar combate em pressao letal";
        } else if (roleSummary.removal() >= 10 && roleSummary.averageCmc() <= 3.4) {
            archetype = "control";
            plan = "controlar a mesa, trocar recursos com eficiencia e vencer com poucas ameacas resilientes";
        } else if (roleSummary.ramp() >= 12) {
            archetype = "midrange";
            plan = "acelerar mana cedo e converter vantagem de recursos em ameacas de alto impacto";
        } else {
            archetype = "value";
            plan = "acumular vantagem incremental, manter consistencia e vencer por qualidade media das cartas";
        }

        LOG.infov(
                "event=deck.archetype.detected commander=\"{0}\" archetype={1} signals={2}",
                commanderName,
                archetype,
                signalTags(allTags)
        );
        return new CommanderArchetypeProfile(commanderName, colors, archetype, plan, commanderTags);
    }

    private List<String> signalTags(Set<String> tags) {
        return tags.stream()
                .filter(tag -> Set.of("stax", "combo-piece", "tutor", "token", "sacrifice", "graveyard", "combat", "counterspell", "selection").contains(tag))
                .sorted()
                .toList();
    }
}
