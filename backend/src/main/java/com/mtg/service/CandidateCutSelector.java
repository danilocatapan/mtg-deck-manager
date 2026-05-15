package com.mtg.service;

import com.mtg.domain.CutScoreBreakdown;
import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.service.synergy.SynergyEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class CandidateCutSelector {
    private static final Logger LOG = Logger.getLogger(CandidateCutSelector.class);

    @Inject
    SynergyEngine synergyEngine;

    @Inject
    CardRoleClassifier roleClassifier;

    @Inject
    ComboDetectionService comboDetectionService;

    @Inject
    RecommendationAuditContext auditContext;

    public List<StrategicCandidate> select(Deck deck, Map<String, CardResponseDTO> cardsByName, CommanderArchetypeProfile profile, DeckRoleSummary roles) {
        return select(deck, cardsByName, profile, roles, "casual");
    }

    public List<StrategicCandidate> select(Deck deck, Map<String, CardResponseDTO> cardsByName, CommanderArchetypeProfile profile, DeckRoleSummary roles, String bracket) {
        return select(deck, cardsByName, profile, roles, bracket, StrategicDeckAssessment.empty());
    }

    public List<StrategicCandidate> select(Deck deck, Map<String, CardResponseDTO> cardsByName, CommanderArchetypeProfile profile, DeckRoleSummary roles, String bracket, StrategicDeckAssessment assessment) {
        Set<String> protectedComboPieces = protectedComboPieces(deck);
        return deck.getCards().stream()
                .filter(deckCard -> !normalize(deckCard.getName()).equals(normalize(deck.getCommander())))
                .filter(deckCard -> canCutComboPiece(deckCard, protectedComboPieces))
                .map(deckCard -> toCandidate(deckCard, fallbackCard(deckCard), cardsByName.get(normalize(deckCard.getName())), profile, roles, bracket, assessment))
                .filter(candidate -> candidate.card() != null)
                .filter(candidate -> !"land".equals(candidate.role()) || roles.lands() > minLandTarget(bracket))
                .filter(candidate -> roleProtectionAllowsCut(candidate.role(), candidate.card(), roles, bracket))
                .filter(candidate -> strategicProtectionAllowsCut(candidate, profile, assessment))
                .sorted(Comparator.comparingDouble(StrategicCandidate::score).reversed())
                .limit(24)
                .toList();
    }

    private StrategicCandidate toCandidate(DeckCard deckCard, CardResponseDTO fallbackCard, CardResponseDTO resolvedCard, CommanderArchetypeProfile profile, DeckRoleSummary roles, String bracket, StrategicDeckAssessment assessment) {
        CardResponseDTO card = resolvedCard == null ? fallbackCard : resolvedCard;

        String role = classifyRole(card);
        double synergy = synergyEngine.computeSynergy(synergyEngine.tagsForCard(card), roles.deckTags(), profile.commanderTags());
        double lowSynergy = 1.0 - synergy;
        double lowEfficiency = lowEfficiency(card);
        double redundancy = redundancy(role, roles);
        double curvePressure = card.cmc() != null && card.cmc() >= 5.0 && roles.averageCmc() > 3.5 ? 1.0 : 0.2;
        double weakRoleFit = weakRoleFit(role, profile);
        double strictUpgrade = strictUpgradeAvailable(role, card);
        double lowMetaFitForBracket = lowMetaFitForBracket(card, bracket);
        double badMultiplayerScaling = badMultiplayerScaling(card, role, profile);
        double manaInefficiency = manaInefficiency(card, bracket);
        double score = lowSynergy * 0.25 + lowEfficiency * 0.20 + curvePressure * 0.15
                + redundancy * 0.12 + lowMetaFitForBracket * 0.13 + strictUpgrade * 0.08
                + badMultiplayerScaling * 0.05 + manaInefficiency * 0.02;
        if ("value".equals(role)) {
            score += weakRoleFit * 0.05;
        }
        if (assessment != null && assessment.isWeakCard(card.name())) {
            score += 0.28;
        }
        double keepValue = strategicKeepValue(card, role, profile, synergy);
        score -= keepValue * 0.32;
        if (isTappedLand(card) && ("high-power".equalsIgnoreCase(bracket) || "cedh".equalsIgnoreCase(bracket))) {
            score += 0.22;
        }
        if (isConditionalDraw(card) && ("high-power".equalsIgnoreCase(bracket) || "cedh".equalsIgnoreCase(bracket))) {
            score += 0.18;
        }

        CutScoreBreakdown breakdown = new CutScoreBreakdown(
                round(score),
                round(lowSynergy),
                round(Math.max(lowEfficiency, curvePressure)),
                round(lowMetaFitForBracket),
                round(redundancy),
                round(badMultiplayerScaling),
                round(manaInefficiency),
                round(keepValue),
                keepValue >= 0.78,
                cutReasons(card, role, assessment, badMultiplayerScaling, manaInefficiency, keepValue)
        );
        StrategicCandidate candidate = new StrategicCandidate(card, role, score, cutReason(role, card, assessment), false, 0.0, synergy, "deck_current", null, breakdown);
        LOG.infov(
                "event=recommendation.cut_candidate.scored card=\"{0}\" role={1} score={2} synergy={3} strategicValue={4} reason=\"{5}\"",
                card.name(),
                role,
                round(score),
                round(synergy),
                round(keepValue),
                candidate.reason()
        );
        LOG.infov(
                "event=recommendation.cut_score.breakdown card=\"{0}\" role={1} cutScore={2} lowSynergy={3} highCmcLowImpact={4} lowMetaFit={5} redundancy={6} badMultiplayer={7} manaInefficiency={8} strategicKeepValue={9}",
                card.name(),
                role,
                breakdown.cutScore(),
                breakdown.lowSynergy(),
                breakdown.highCmcLowImpact(),
                breakdown.lowMetaFit(),
                breakdown.roleRedundancy(),
                breakdown.badMultiplayerScaling(),
                breakdown.manaInefficiency(),
                breakdown.strategicKeepValue()
        );
        return candidate;
    }

    private CardResponseDTO fallbackCard(DeckCard deckCard) {
        String name = deckCard == null ? null : deckCard.getName();
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = normalize(name);
        if (isBasicLand(normalized)) {
            return new CardResponseDTO(name, "", "Basic Land", "Add one mana.", 0.0, List.of(), List.of());
        }
        return new CardResponseDTO(name, "", "", "", 4.0, List.of(), List.of());
    }

    private boolean isBasicLand(String normalizedName) {
        return Set.of("plains", "island", "swamp", "mountain", "forest", "wastes").contains(normalizedName);
    }

    private String classifyRole(CardResponseDTO card) {
        return classifier().primaryRole(card);
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
        if (isCombat(profile.archetype()) && ("finisher".equals(role) || "protection".equals(role) || "ramp".equals(role))) return 0.1;
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

    private double lowMetaFitForBracket(CardResponseDTO card, String bracket) {
        double cmc = card.cmc() != null ? card.cmc() : 0.0;
        String oracle = text(card.oracleText());
        String normalizedBracket = bracket == null ? "casual" : bracket.toLowerCase(Locale.ROOT);
        if ("cedh".equals(normalizedBracket)) {
            return cmc >= 4.0 && !oracle.contains("win the game") && !oracle.contains("counter target") ? 1.0 : 0.25;
        }
        if ("high-power".equals(normalizedBracket)) {
            return cmc >= 5.0 || oracle.contains("at the beginning of your upkeep") ? 0.85 : 0.3;
        }
        if ("casual".equals(normalizedBracket)) {
            return cmc >= 7.0 ? 0.65 : 0.35;
        }
        return cmc >= 5.0 ? 0.65 : 0.35;
    }

    private boolean isTappedLand(CardResponseDTO card) {
        String name = normalize(card.name());
        String oracle = text(card.oracleText());
        String type = text(card.typeLine());
        return type.contains("land") && (oracle.contains("enters tapped")
                || name.contains("temple of")
                || name.contains("rugged highlands")
                || name.contains("guildgate")
                || name.contains("turf")
                || name.contains("chancery")
                || name.contains("aqueduct")
                || name.contains("basilica")
                || name.contains("garrison"));
    }

    private boolean isConditionalDraw(CardResponseDTO card) {
        String name = normalize(card.name());
        String oracle = text(card.oracleText());
        return oracle.contains("draw")
                && (oracle.contains("combat damage")
                || oracle.contains("equal to")
                || oracle.contains("deals damage")
                || name.contains("soul's majesty")
                || name.contains("hunter's insight"));
    }

    private double badMultiplayerScaling(CardResponseDTO card, String role, CommanderArchetypeProfile profile) {
        String oracle = text(card.oracleText());
        double cmc = card.cmc() == null ? 0.0 : card.cmc();
        double score = 0.2;
        if (cmc >= 6.0 && !oracle.contains("each opponent") && !oracle.contains("additional combat") && !oracle.contains("draw")) score += 0.35;
        if (isConditionalDraw(card)) score += 0.28;
        if ("finisher".equals(role) && !hasCombatLethality(card) && !oracle.contains("when this creature enters")) score += 0.25;
        if (oracle.contains("target creature") && !oracle.contains("each")) score += 0.1;
        if (oracle.contains("each opponent") || oracle.contains("additional combat") || oracle.contains("indestructible")) score -= 0.25;
        return clamp(score);
    }

    private double manaInefficiency(CardResponseDTO card, String bracket) {
        String normalizedBracket = bracket == null ? "casual" : bracket.toLowerCase(Locale.ROOT);
        double cmc = card.cmc() == null ? 0.0 : card.cmc();
        if (isTappedLand(card)) {
            return "high-power".equals(normalizedBracket) || "cedh".equals(normalizedBracket) ? 1.0 : 0.55;
        }
        if ("high-power".equals(normalizedBracket) || "cedh".equals(normalizedBracket)) {
            if (cmc >= 4.0 && text(card.oracleText()).contains("add ")) return 0.9;
            if (cmc >= 3.0 && text(card.oracleText()).contains("search your library") && text(card.oracleText()).contains("basic")) return 0.75;
        }
        return 0.2;
    }

    private List<String> cutReasons(CardResponseDTO card, String role, StrategicDeckAssessment assessment, double badMultiplayerScaling, double manaInefficiency, double keepValue) {
        java.util.ArrayList<String> reasons = new java.util.ArrayList<>();
        if (assessment != null && assessment.isWeakCard(card.name())) reasons.add("structural_weakness");
        if (badMultiplayerScaling >= 0.65) reasons.add("bad_multiplayer_scaling");
        if (manaInefficiency >= 0.65) reasons.add("mana_inefficiency");
        if (keepValue >= 0.78) reasons.add("strategic_keep_value");
        if ("finisher".equals(role) && !hasCombatLethality(card)) reasons.add("low_lethality_finisher");
        if (reasons.isEmpty()) reasons.add("lower_priority_slot");
        return reasons;
    }

    private boolean roleProtectionAllowsCut(String role, CardResponseDTO card, DeckRoleSummary roles, String bracket) {
        String normalizedBracket = bracket == null ? "casual" : bracket.toLowerCase(Locale.ROOT);
        int rampFloor = "cedh".equals(normalizedBracket) ? 14 : "high-power".equals(normalizedBracket) ? 12 : "mid".equals(normalizedBracket) ? 10 : 9;
        int drawFloor = "cedh".equals(normalizedBracket) ? 12 : "high-power".equals(normalizedBracket) ? 10 : "mid".equals(normalizedBracket) ? 9 : 8;
        int removalFloor = "cedh".equals(normalizedBracket) ? 14 : "high-power".equals(normalizedBracket) ? 10 : "mid".equals(normalizedBracket) ? 8 : 7;
        int protectionFloor = "high-power".equals(normalizedBracket) || "cedh".equals(normalizedBracket) ? 4 : 2;
        return switch (role) {
            case "ramp" -> roles.ramp() > rampFloor;
            case "draw" -> roles.draw() > drawFloor;
            case "removal" -> roles.removal() > removalFloor;
            case "protection" -> roles.protection() > protectionFloor;
            case "finisher" -> roles.finishers() > 1 || isGenericExpensiveThreat(card);
            default -> true;
        };
    }

    private boolean strategicProtectionAllowsCut(StrategicCandidate candidate, CommanderArchetypeProfile profile, StrategicDeckAssessment assessment) {
        if (candidate == null || candidate.card() == null) {
            return false;
        }
        double keepValue = strategicKeepValue(candidate.card(), candidate.role(), profile, candidate.synergyEstimate());
        if (keepValue >= 0.78) {
            LOG.infov(
                    "event=cut.protected reason=strategic_value card=\"{0}\" role={1} archetype={2} strategicValue={3} synergy={4}",
                    candidate.card().name(),
                    candidate.role(),
                    profile == null ? "" : profile.archetype(),
                    round(keepValue),
                    round(candidate.synergyEstimate())
            );
            if (auditContext != null) {
                auditContext.recordProtectedCut(candidate.card().name(), candidate.role(), "strategic_value", round(keepValue), round(candidate.synergyEstimate()));
            }
            return false;
        }
        if (assessment != null && assessment.isWeakCard(candidate.card().name())) {
            LOG.infov(
                    "event=cut.allowed reason=structural_weakness card=\"{0}\" role={1} strategicValue={2}",
                    candidate.card().name(),
                    candidate.role(),
                    round(keepValue)
            );
            return true;
        }
        return true;
    }

    private double strategicKeepValue(CardResponseDTO card, String role, CommanderArchetypeProfile profile, double synergy) {
        String oracle = text(card.oracleText());
        String type = text(card.typeLine());
        String archetype = profile == null ? "" : profile.archetype();
        double value = 0.0;

        if ("combo-piece".equals(role)) value += 0.85;
        if ("finisher".equals(role)) value += 0.38;
        if ("draw".equals(role) && (oracle.contains("whenever") || oracle.contains("sacrifice"))) value += 0.18;
        if ("protection".equals(role) && (oracle.contains("indestructible") || oracle.contains("hexproof"))) value += 0.18;

        if (isCombat(archetype) || "voltron".equals(archetype)) {
            if (hasCombatLethality(card)) value += 0.38;
            if (oracle.contains("additional combat") || oracle.contains("untap all attacking creatures")) value += 0.32;
            if (oracle.contains("double strike") || oracle.contains("infect")) value += 0.28;
            if (oracle.contains("trample") || oracle.contains("flying") || oracle.contains("can't be blocked")) value += 0.12;
            if (oracle.contains("gets +") || oracle.contains("+x/+x") || oracle.contains("damage to a player")) value += 0.12;
        }
        if ("tokens".equals(archetype) && (oracle.contains("token") || oracle.contains("creatures you control get"))) value += 0.3;
        if ("aristocrats".equals(archetype) && (oracle.contains("sacrifice") || oracle.contains("dies") || oracle.contains("loses 1 life"))) value += 0.32;
        if ("reanimator".equals(archetype) && (oracle.contains("graveyard") || oracle.contains("return target creature"))) value += 0.32;
        if ("spellslinger".equals(archetype) && (oracle.contains("instant or sorcery") || oracle.contains("magecraft") || oracle.contains("storm"))) value += 0.32;
        if (("control".equals(archetype) || "stax".equals(archetype))
                && (oracle.contains("counter target") || oracle.contains("can't cast") || oracle.contains("cost") || oracle.contains("draw a card"))) {
            value += 0.28;
        }

        if (synergy >= 0.65) value += 0.18;
        else if (synergy >= 0.4) value += 0.10;
        if (type.contains("legendary") && value >= 0.45) value += 0.06;

        return Math.min(1.0, value);
    }

    private boolean isGenericExpensiveThreat(CardResponseDTO card) {
        double cmc = card.cmc() != null ? card.cmc() : 0.0;
        String oracle = text(card.oracleText());
        return cmc >= 5.0
                && !oracle.contains("win the game")
                && !oracle.contains("extra turn")
                && !oracle.contains("combo")
                && !oracle.contains("whenever")
                && !hasCombatLethality(card);
    }

    private boolean hasCombatLethality(CardResponseDTO card) {
        String oracle = text(card.oracleText());
        return oracle.contains("double strike")
                || oracle.contains("infect")
                || oracle.contains("additional combat")
                || oracle.contains("deals combat damage to a player")
                || oracle.contains("whenever this creature attacks")
                || oracle.contains("attacking creatures")
                || oracle.contains("damage as though it weren't blocked");
    }

    private int minLandTarget(String bracket) {
        String normalizedBracket = bracket == null ? "casual" : bracket.toLowerCase(Locale.ROOT);
        return switch (normalizedBracket) {
            case "cedh" -> 27;
            case "high-power" -> 30;
            case "mid" -> 34;
            default -> 36;
        };
    }

    private String cutReason(String role, CardResponseDTO card, StrategicDeckAssessment assessment) {
        double cmc = card.cmc() != null ? card.cmc() : 0.0;
        String structural = assessment != null && assessment.isWeakCard(card.name())
                ? " e foi marcado como fraqueza estrutural da lista"
                : "";
        return switch (role) {
            case "ramp" -> "é uma peça de ramp mais lenta ou redundante para a curva atual";
            case "draw" -> "ocupa slot de compra com velocidade ou escala inferiores";
            case "removal" -> "é interação menos eficiente que pode virar um upgrade direto";
            case "finisher" -> "pressiona a curva e precisa justificar muito impacto";
            case "land" -> "só deve sair se a contagem de terrenos estiver acima do necessário";
            default -> cmc >= 4.0 ? "tem impacto genérico e custo alto para o plano" : "contribui pouco para a estratégia principal";
        } + structural;
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> protectedComboPieces(Deck deck) {
        ComboDetectionService service = comboDetectionService == null ? new ComboDetectionService() : comboDetectionService;
        return service.protectedPieces(deck.getCards().stream()
                .map(DeckCard::getName)
                .collect(java.util.stream.Collectors.toSet()));
    }

    private boolean canCutComboPiece(DeckCard deckCard, Set<String> protectedComboPieces) {
        String normalizedName = normalize(deckCard.getName());
        if (!protectedComboPieces.contains(normalizedName)) {
            return true;
        }
        LOG.infov("event=cut.protected reason=combo_piece card=\"{0}\"", deckCard.getName());
        return false;
    }

    private CardRoleClassifier classifier() {
        return roleClassifier == null ? new CardRoleClassifier() : roleClassifier;
    }

    private boolean isCombat(String archetype) {
        return "combat".equals(archetype) || "combat damage".equals(archetype);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
