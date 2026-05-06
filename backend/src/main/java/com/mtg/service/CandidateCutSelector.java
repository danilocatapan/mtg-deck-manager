package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.service.synergy.SynergyEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class CandidateCutSelector {

    @Inject
    SynergyEngine synergyEngine;

    public List<StrategicCandidate> select(Deck deck, Map<String, CardResponseDTO> cardsByName, CommanderArchetypeProfile profile, DeckRoleSummary roles) {
        return deck.getCards().stream()
                .map(deckCard -> toCandidate(deckCard, cardsByName.get(normalize(deckCard.getName())), profile, roles))
                .filter(candidate -> candidate.card() != null)
                .filter(candidate -> !"land".equals(candidate.role()) || roles.lands() > 37)
                .sorted(Comparator.comparingDouble(StrategicCandidate::score).reversed())
                .limit(12)
                .toList();
    }

    private StrategicCandidate toCandidate(DeckCard deckCard, CardResponseDTO card, CommanderArchetypeProfile profile, DeckRoleSummary roles) {
        if (card == null) {
            return new StrategicCandidate(null, "unknown", 0.0, "sem dados suficientes para avaliar");
        }

        String role = classifyRole(card);
        double lowSynergy = 1.0 - synergyEngine.computeSynergy(synergyEngine.tagsForCard(card), roles.deckTags(), profile.commanderTags());
        double lowEfficiency = lowEfficiency(card);
        double redundancy = redundancy(role, roles);
        double curvePressure = card.cmc() != null && card.cmc() >= 5.0 && roles.averageCmc() > 3.5 ? 1.0 : 0.2;
        double weakRoleFit = weakRoleFit(role, profile);
        double strictUpgrade = strictUpgradeAvailable(role, card);
        double score = lowSynergy * 0.30 + lowEfficiency * 0.20 + redundancy * 0.15
                + curvePressure * 0.15 + weakRoleFit * 0.10 + strictUpgrade * 0.10;

        return new StrategicCandidate(card, role, score, cutReason(role, card));
    }

    private String classifyRole(CardResponseDTO card) {
        String oracle = text(card.oracleText());
        String type = text(card.typeLine());
        if (type.contains("land")) return "land";
        if (oracle.contains("indestructible") || oracle.contains("hexproof") || oracle.contains("phase out") || oracle.contains("protection")) return "protection";
        if (oracle.contains("draw")) return "draw";
        if (oracle.contains("destroy") || oracle.contains("exile") || oracle.contains("counter target")) return "removal";
        if (oracle.contains("add ") || oracle.contains("search your library for a land")) return "ramp";
        if (type.contains("creature") && card.cmc() != null && card.cmc() >= 5.0) return "finisher";
        return "value";
    }

    private double lowEfficiency(CardResponseDTO card) {
        double cmc = card.cmc() != null ? card.cmc() : 0.0;
        String oracle = text(card.oracleText());
        if (cmc >= 5.0 && !(oracle.contains("win the game") || oracle.contains("extra turn"))) return 0.9;
        if (cmc >= 4.0 && oracle.contains("draw a card")) return 0.8;
        if (cmc <= 2.0) return 0.1;
        return 0.45;
    }

    private double redundancy(String role, DeckRoleSummary roles) {
        return switch (role) {
            case "ramp" -> roles.ramp() > 13 ? 0.8 : 0.2;
            case "draw" -> roles.draw() > 11 ? 0.8 : 0.25;
            case "removal" -> roles.removal() > 10 ? 0.7 : 0.2;
            case "finisher" -> roles.finishers() > 7 ? 0.8 : 0.35;
            default -> 0.5;
        };
    }

    private double weakRoleFit(String role, CommanderArchetypeProfile profile) {
        if ("combat damage".equals(profile.archetype()) && ("finisher".equals(role) || "protection".equals(role) || "ramp".equals(role))) return 0.1;
        if ("control".equals(profile.archetype()) && ("removal".equals(role) || "draw".equals(role))) return 0.1;
        if ("tokens".equals(profile.archetype()) && "finisher".equals(role)) return 0.65;
        return "value".equals(role) ? 0.6 : 0.35;
    }

    private double strictUpgradeAvailable(String role, CardResponseDTO card) {
        double cmc = card.cmc() != null ? card.cmc() : 0.0;
        return switch (role) {
            case "ramp" -> cmc >= 3.0 ? 0.8 : 0.2;
            case "draw" -> cmc >= 4.0 ? 0.75 : 0.25;
            case "removal" -> cmc >= 4.0 ? 0.7 : 0.2;
            default -> cmc >= 5.0 ? 0.6 : 0.3;
        };
    }

    private String cutReason(String role, CardResponseDTO card) {
        double cmc = card.cmc() != null ? card.cmc() : 0.0;
        return switch (role) {
            case "ramp" -> "é uma peça de ramp mais lenta ou redundante para a curva atual";
            case "draw" -> "ocupa slot de compra com velocidade ou escala inferiores";
            case "removal" -> "é interação menos eficiente que pode virar um upgrade direto";
            case "finisher" -> "pressiona a curva e precisa justificar muito impacto";
            case "land" -> "só deve sair se a contagem de terrenos estiver acima do necessário";
            default -> cmc >= 4.0 ? "tem impacto genérico e custo alto para o plano" : "contribui pouco para a estratégia principal";
        };
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
