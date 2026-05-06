package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.service.meta.MetaCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class CandidateAddSelector {

    private static final Set<String> COMMANDER_BANNED = Set.of(
            "ancestral recall", "balance", "black lotus", "channel", "chaos orb", "falling star",
            "flash", "golos, tireless pilgrim", "hullbreacher", "iona, shield of emeria",
            "leovold, emissary of trest", "limited resources", "lutri, the spellchaser",
            "mox emerald", "mox jet", "mox pearl", "mox ruby", "mox sapphire", "primeval titan",
            "prophet of kruphix", "recurring nightmare", "rofelos, llanowar emissary",
            "sway of the stars", "sylvan primordial", "time vault", "time walk", "tinker",
            "tolarian academy", "trade secrets", "upheaval"
    );

    @Inject
    CardService cardService;

    @Inject
    com.mtg.service.synergy.SynergyEngine synergyEngine;

    public List<StrategicCandidate> select(
            Deck deck,
            List<MetaCard> metaCards,
            Map<String, CardResponseDTO> knownCards,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            String bracket
    ) {
        Set<String> existingNames = new HashSet<>();
        deck.getCards().stream().map(DeckCard::getName).map(this::normalize).forEach(existingNames::add);
        List<CardResponseDTO> cards = new ArrayList<>();

        for (MetaCard metaCard : metaCards) {
            CardResponseDTO card = knownCards.get(normalize(metaCard.getName()));
            if (isLegalAdd(card, existingNames, profile.colors())) {
                cards.add(card);
            }
            if (cards.size() >= 30) break;
        }

        if (cards.size() < 12) {
            for (String role : prioritizedGapRoles(roles, profile)) {
                for (CardResponseDTO card : fallbackCards(role)) {
                    knownCards.putIfAbsent(normalize(card.name()), card);
                    if (isLegalAdd(card, existingNames, profile.colors())) {
                        cards.add(card);
                    }
                }
            }
        }

        return cards.stream()
                .map(card -> toCandidate(card, metaCards, profile, roles, bracket))
                .sorted(Comparator.comparingDouble(StrategicCandidate::score).reversed())
                .distinct()
                .limit(12)
                .toList();
    }

    private StrategicCandidate toCandidate(CardResponseDTO card, List<MetaCard> metaCards, CommanderArchetypeProfile profile, DeckRoleSummary roles, String bracket) {
        String role = classifyRole(card);
        double commanderSynergy = synergyEngine.computeSynergy(synergyEngine.tagsForCard(card), roles.deckTags(), profile.commanderTags());
        double gapScore = roles.gaps().containsKey(role) || ("curve".equals(role) && roles.gaps().containsKey("curve")) ? 1.0 : 0.3;
        double efficiency = efficiency(card);
        double archetypeFit = archetypeFit(card, profile);
        MetaCard metaCard = metaCards.stream()
                .filter(meta -> meta.getName().equalsIgnoreCase(card.name()))
                .findFirst()
                .orElse(null);
        double metaScore = metaCard == null ? 0.2 : Math.min(1.0, metaCard.getInclusion() * metaCard.getBracketWeight() + metaCard.getPerformanceWeight());
        double bracketFit = bracketFit(card, bracket, metaCard);
        double score = scoreForBracket(bracket, commanderSynergy, gapScore, efficiency, archetypeFit, metaScore, bracketFit, role);
        return new StrategicCandidate(card, role, score, addReason(role, profile));
    }

    private List<String> prioritizedGapRoles(DeckRoleSummary roles, CommanderArchetypeProfile profile) {
        List<String> result = new ArrayList<>(roles.gaps().keySet());
        if (result.isEmpty()) {
            result.addAll(List.of("draw", "ramp", "removal", "protection", "finisher"));
        }
        if ("combat damage".equals(profile.archetype()) && !result.contains("protection")) {
            result.add("protection");
        }
        return result;
    }

    private List<CardResponseDTO> fallbackCards(String role) {
        String query = switch (role) {
            case "ramp", "curve" -> "oracle:\"search your library for a land\" OR oracle:\"add {\"";
            case "draw" -> "oracle:draw";
            case "removal" -> "oracle:destroy OR oracle:exile";
            case "protection" -> "oracle:hexproof OR oracle:indestructible OR oracle:\"phase out\"";
            case "finisher" -> "type:creature cmc>=5";
            default -> "";
        };
        if (query.isBlank()) {
            return List.of();
        }
        try {
            return cardService.searchByQuery(query).stream().limit(20).toList();
        } catch (ExternalServiceException exception) {
            return List.of();
        }
    }

    private boolean isLegalAdd(CardResponseDTO card, Set<String> existingNames, Set<String> commanderColors) {
        if (card == null || card.name() == null) return false;
        String normalizedName = normalize(card.name());
        if (existingNames.contains(normalizedName)) return false;
        if (COMMANDER_BANNED.contains(normalizedName)) return false;
        if (!ColorIdentityMatcher.matches(card, commanderColors)) return false;
        String typeLine = card.typeLine() == null ? "" : card.typeLine().toLowerCase(Locale.ROOT);
        return !typeLine.contains("plane") && !typeLine.contains("scheme") && !typeLine.contains("conspiracy");
    }

    private String classifyRole(CardResponseDTO card) {
        String oracle = text(card.oracleText());
        String type = text(card.typeLine());
        if (oracle.contains("indestructible") || oracle.contains("hexproof") || oracle.contains("phase out") || oracle.contains("protection")) return "protection";
        if (oracle.contains("draw")) return "draw";
        if (oracle.contains("destroy") || oracle.contains("exile") || oracle.contains("counter target")) return "removal";
        if (oracle.contains("add ") || oracle.contains("search your library for a land")) return "ramp";
        if (type.contains("creature") && card.cmc() != null && card.cmc() >= 5.0) return "finisher";
        return "value";
    }

    private double efficiency(CardResponseDTO card) {
        double cmc = card.cmc() != null ? card.cmc() : 0.0;
        if (cmc <= 2.0) return 1.0;
        if (cmc <= 3.0) return 0.8;
        if (cmc <= 5.0) return 0.55;
        return 0.3;
    }

    private double archetypeFit(CardResponseDTO card, CommanderArchetypeProfile profile) {
        String oracle = text(card.oracleText());
        if ("combat damage".equals(profile.archetype()) && (oracle.contains("trample") || oracle.contains("combat") || oracle.contains("double strike"))) return 1.0;
        if ("tokens".equals(profile.archetype()) && oracle.contains("token")) return 1.0;
        if ("aristocrats".equals(profile.archetype()) && (oracle.contains("sacrifice") || oracle.contains("graveyard"))) return 1.0;
        if ("control".equals(profile.archetype()) && (oracle.contains("counter target") || oracle.contains("draw") || oracle.contains("exile"))) return 0.9;
        return 0.55;
    }

    private double bracketFit(CardResponseDTO card, String bracket, MetaCard metaCard) {
        double cmc = card.cmc() != null ? card.cmc() : 0.0;
        String source = metaCard == null ? "LOCAL" : metaCard.getSource();
        if ("casual".equalsIgnoreCase(bracket)) {
            if ("EDHTOP16".equalsIgnoreCase(source) || "TOPDECK".equalsIgnoreCase(source)) return 0.45;
            return cmc <= 4.0 ? 0.9 : 0.55;
        }
        if ("mid".equalsIgnoreCase(bracket)) return cmc <= 4.0 ? 0.85 : 0.55;
        if ("high-power".equalsIgnoreCase(bracket)) return cmc <= 3.0 ? 1.0 : 0.45;
        if ("cedh".equalsIgnoreCase(bracket)) return cmc <= 2.0 || isStackInteraction(card) ? 1.0 : 0.35;
        return 0.6;
    }

    private double scoreForBracket(
            String bracket,
            double synergy,
            double gapFix,
            double efficiency,
            double archetypeFit,
            double meta,
            double bracketFit,
            String role
    ) {
        double comboOrInteractionDensity = ("removal".equals(role) || "protection".equals(role)) ? 1.0 : 0.45;
        return switch (bracket == null ? "casual" : bracket.toLowerCase(Locale.ROOT)) {
            case "cedh" -> meta * 0.35 + efficiency * 0.30 + comboOrInteractionDensity * 0.15 + synergy * 0.10 + gapFix * 0.10;
            case "high-power" -> efficiency * 0.30 + meta * 0.25 + synergy * 0.20 + gapFix * 0.15 + archetypeFit * 0.10;
            case "mid" -> synergy * 0.30 + gapFix * 0.25 + efficiency * 0.20 + meta * 0.15 + archetypeFit * 0.10;
            default -> synergy * 0.35 + gapFix * 0.25 + archetypeFit * 0.20 + efficiency * 0.10 + Math.min(meta, bracketFit) * 0.10;
        };
    }

    private boolean isStackInteraction(CardResponseDTO card) {
        String oracle = text(card.oracleText());
        return oracle.contains("counter target") || oracle.contains("you may cast") || oracle.contains("mana value 1 or less");
    }

    private String addReason(String role, CommanderArchetypeProfile profile) {
        return switch (role) {
            case "draw" -> "aumenta card advantage sem fugir do plano " + profile.archetype();
            case "ramp" -> "melhora a curva inicial e acelera o plano " + profile.archetype();
            case "removal" -> "aumenta interação para proteger o plano principal";
            case "protection" -> "protege comandante ou peças-chave do plano";
            case "finisher" -> "converte recursos acumulados em condição de vitória";
            default -> "aumenta consistência e sinergia geral";
        };
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
