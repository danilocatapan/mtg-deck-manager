package com.mtg.service;

import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.model.DeckCard;
import com.mtg.service.rules.CommanderBanlistService;
import com.mtg.service.meta.MetaCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class CandidateAddSelector {
    private static final Logger LOG = Logger.getLogger(CandidateAddSelector.class);

    @Inject
    CardService cardService;

    @Inject
    com.mtg.service.synergy.SynergyEngine synergyEngine;

    @Inject
    CommanderBanlistService commanderBanlistService;

    @Inject
    CardRoleClassifier roleClassifier;

    @Inject
    ComboDetectionService comboDetectionService;

    public List<StrategicCandidate> select(
            Deck deck,
            List<MetaCard> metaCards,
            Map<String, CardResponseDTO> knownCards,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            String bracket
    ) {
        return select(deck, metaCards, knownCards, profile, roles, bracket, true);
    }

    public List<StrategicCandidate> select(
            Deck deck,
            List<MetaCard> metaCards,
            Map<String, CardResponseDTO> knownCards,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            String bracket,
            boolean metaProfileDriven
    ) {
        return select(deck, metaCards, knownCards, profile, roles, bracket, metaProfileDriven, null, null);
    }

    public List<StrategicCandidate> select(
            Deck deck,
            List<MetaCard> metaCards,
            Map<String, CardResponseDTO> knownCards,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            String bracket,
            boolean metaProfileDriven,
            String recommendationMode,
            Double budget
    ) {
        return select(deck, metaCards, knownCards, profile, roles, bracket, metaProfileDriven, recommendationMode, budget, Set.of());
    }

    public List<StrategicCandidate> select(
            Deck deck,
            List<MetaCard> metaCards,
            Map<String, CardResponseDTO> knownCards,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            String bracket,
            boolean metaProfileDriven,
            String recommendationMode,
            Double budget,
            Set<String> filters
    ) {
        return select(deck, metaCards, knownCards, profile, roles, bracket, metaProfileDriven, recommendationMode, budget, filters, StrategicDeckAssessment.empty());
    }

    public List<StrategicCandidate> select(
            Deck deck,
            List<MetaCard> metaCards,
            Map<String, CardResponseDTO> knownCards,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            String bracket,
            boolean metaProfileDriven,
            String recommendationMode,
            Double budget,
            Set<String> filters,
            StrategicDeckAssessment assessment
    ) {
        Set<String> existingNames = new HashSet<>();
        deck.getCards().stream()
                .map(DeckCard::getName)
                .map(this::normalize)
                .forEach(existingNames::add);
        List<CardResponseDTO> cards = new ArrayList<>();
        Map<String, String> comboSignals = comboSignals(deck);

        for (MetaCard metaCard : metaCards.stream().limit(50).toList()) {
            CardResponseDTO card = knownCards.get(normalize(metaCard.getName()));
            addIfLegal(cards, card, existingNames, profile.colors(), filters);
            if (cards.size() >= 30) break;
        }

        for (String missingComboCard : comboSignals.keySet()) {
            CardResponseDTO card = knownCards.get(missingComboCard);
            if (card == null) {
                card = strategicPoolCard(missingComboCard);
                if (card != null) {
                    knownCards.putIfAbsent(normalize(card.name()), card);
                }
            }
            addIfLegal(cards, card, existingNames, profile.colors(), filters);
        }

        for (CardResponseDTO card : archetypeFallbackCards(profile, bracket)) {
            knownCards.putIfAbsent(normalize(card.name()), card);
            if (cards.size() >= 30) break;
            addIfLegal(cards, card, existingNames, profile.colors(), filters);
        }

        if (cards.size() < 12) {
            for (String role : prioritizedGapRoles(roles, profile)) {
                for (CardResponseDTO card : fallbackCards(role, bracket)) {
                    knownCards.putIfAbsent(normalize(card.name()), card);
                    addIfLegal(cards, card, existingNames, profile.colors(), filters);
                }
            }
        }

        return cards.stream()
                .map(card -> toCandidate(card, metaCards, profile, roles, bracket, metaProfileDriven, recommendationMode, budget, filters, comboSignals, assessment))
                .sorted(Comparator.comparingDouble(StrategicCandidate::score).reversed())
                .distinct()
                .limit(12)
                .toList();
    }

    private void addIfLegal(List<CardResponseDTO> cards, CardResponseDTO card, Set<String> existingNames, Set<String> commanderColors, Set<String> filters) {
        if (isLegalAdd(card, existingNames, commanderColors) && passesFilters(card, filters)
                && cards.stream().noneMatch(existing -> normalize(existing.name()).equals(normalize(card.name())))) {
            cards.add(card);
        }
    }

    private StrategicCandidate toCandidate(CardResponseDTO card, List<MetaCard> metaCards, CommanderArchetypeProfile profile, DeckRoleSummary roles, String bracket, boolean metaProfileDriven, String recommendationMode, Double budget, Set<String> filters, Map<String, String> comboSignals, StrategicDeckAssessment assessment) {
        String role = classifyRole(card);
        double commanderSynergy = synergyEngine.computeSynergy(synergyEngine.tagsForCard(card), roles.deckTags(), profile.commanderTags());
        double gapScore = roles.gaps().containsKey(role) || ("curve".equals(role) && roles.gaps().containsKey("curve")) ? 1.0 : 0.3;
        double efficiency = efficiency(card);
        double archetypeFit = archetypeFit(card, profile);
        MetaCard metaCard = metaCards.stream()
                .filter(meta -> meta.getName().equalsIgnoreCase(card.name()))
                .findFirst()
                .orElse(null);
        double inclusionRate = metaCard == null ? 0.0 : Math.min(1.0, metaCard.getInclusion());
        double metaScore = metaCard == null ? 0.2 : Math.min(1.0, inclusionRate * metaCard.getBracketWeight() + metaCard.getPerformanceWeight());
        double bracketFit = bracketFit(card, bracket, metaCard);
        double score = scoreForBracket(bracket, commanderSynergy, gapScore, efficiency, archetypeFit, metaScore, bracketFit, role);
        String comboName = comboSignals.get(normalize(card.name()));
        if (comboName != null) {
            score += isCompetitiveBracket(bracket) ? 0.35 : 0.22;
            role = "combo-piece";
            LOG.infov("event=combo.recommendation.signal missingCard=\"{0}\" combo=\"{1}\"", card.name(), comboName);
        }
        score = applyIntent(score, recommendationMode, budget, card, role, commanderSynergy, gapScore, efficiency, archetypeFit, metaScore);
        score = applyFilters(score, filters, card, role, commanderSynergy, archetypeFit);
        score *= assessment == null ? 1.0 : assessment.priorityFor(role);
        String source = comboName != null && metaCard == null
                ? "combo_database"
                : metaProfileDriven && metaCard != null ? nullSafeSource(metaCard.getSource()) : "heuristic_fallback";
        String reason = comboName == null ? addReason(role, profile, assessment) : "completa o combo " + comboName;
        return new StrategicCandidate(card, role, score, reason, metaProfileDriven && metaCard != null, inclusionRate, commanderSynergy, source);
    }

    private Map<String, String> comboSignals(Deck deck) {
        ComboDetectionService service = comboDetectionService == null ? new ComboDetectionService() : comboDetectionService;
        return service.completionSignals(deck.getCards().stream().map(DeckCard::getName).collect(java.util.stream.Collectors.toSet()))
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        signal -> normalize(signal.missingCard()),
                        ComboDetectionService.ComboCompletionSignal::comboName,
                        (first, ignored) -> first
                ));
    }

    private boolean passesFilters(CardResponseDTO card, Set<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        String oracle = text(card == null ? null : card.oracleText());
        String type = text(card == null ? null : card.typeLine());
        if (filters.contains("avoid-tutors") && (oracle.contains("search your library") && !oracle.contains("land card"))) {
            return false;
        }
        if (filters.contains("avoid-salt") && (
                oracle.contains("extra turn")
                        || oracle.contains("can't cast spells")
                        || oracle.contains("skip their untap")
                        || oracle.contains("destroy all lands")
                        || oracle.contains("win the game")
                        || type.contains("sticker")
        )) {
            return false;
        }
        return true;
    }

    private double applyFilters(double score, Set<String> filters, CardResponseDTO card, String role, double synergy, double archetypeFit) {
        if (filters == null || filters.isEmpty()) {
            return score;
        }
        double adjusted = score;
        double cmc = card.cmc() == null ? 0.0 : card.cmc();
        String oracle = text(card.oracleText());
        if (filters.contains("improve-mana") && ("ramp".equals(role) || oracle.contains("any color") || oracle.contains("search your library for a land"))) {
            adjusted *= 1.22;
        }
        if (filters.contains("lower-curve")) {
            adjusted *= cmc <= 2.0 ? 1.18 : cmc >= 5.0 ? 0.72 : 1.0;
        }
        if (filters.contains("more-interaction") && ("removal".equals(role) || "protection".equals(role))) {
            adjusted *= 1.2;
        }
        if (filters.contains("preserve-theme")) {
            adjusted = adjusted * 0.75 + Math.max(synergy, archetypeFit) * 0.25;
        }
        return adjusted;
    }

    private double applyIntent(double score, String mode, Double budget, CardResponseDTO card, String role, double synergy, double gapScore, double efficiency, double archetypeFit, double metaScore) {
        String normalizedMode = mode == null || mode.isBlank() ? "consistency" : mode.toLowerCase(Locale.ROOT);
        double adjusted = score;
        switch (normalizedMode) {
            case "budget", "mais barato" -> adjusted = adjusted * 0.82 + budgetFit(card, budget) * 0.18;
            case "competitive", "mais competitivo" -> adjusted = adjusted * 0.65 + efficiency * 0.20 + metaScore * 0.15;
            case "theme", "mais fiel ao tema" -> adjusted = adjusted * 0.60 + archetypeFit * 0.25 + synergy * 0.15;
            case "casual", "mais casual" -> adjusted = adjusted * 0.72 + Math.min(synergy, archetypeFit) * 0.18 + casualFit(card) * 0.10;
            default -> adjusted = adjusted * 0.85 + gapScore * 0.10 + efficiency * 0.05;
        }
        if ("budget".equals(normalizedMode) && budget != null && card.estimatedPrice() != null && card.estimatedPrice() > budget) {
            adjusted *= 0.55;
        }
        if ("theme".equals(normalizedMode) && "value".equals(role)) {
            adjusted *= 0.92;
        }
        return adjusted;
    }

    private double budgetFit(CardResponseDTO card, Double budget) {
        Double price = card.estimatedPrice();
        if (price == null) {
            return 0.55;
        }
        if (budget == null || budget <= 0.0) {
            return price <= 2.0 ? 1.0 : price <= 8.0 ? 0.75 : 0.35;
        }
        return price <= budget ? 1.0 : Math.max(0.15, budget / price);
    }

    private double casualFit(CardResponseDTO card) {
        double cmc = card.cmc() == null ? 0.0 : card.cmc();
        String oracle = text(card.oracleText());
        if (oracle.contains("win the game") || oracle.contains("extra turn")) {
            return 0.25;
        }
        return cmc <= 5.0 ? 0.85 : 0.55;
    }

    private String nullSafeSource(String source) {
        return source == null || source.isBlank() ? "LOCAL" : source;
    }

    private List<String> prioritizedGapRoles(DeckRoleSummary roles, CommanderArchetypeProfile profile) {
        List<String> result = new ArrayList<>(roles.gaps().keySet());
        for (String role : List.of("draw", "ramp", "removal", "protection", "finisher")) {
            if (!result.contains(role)) {
                result.add(role);
            }
        }
        if (isCombat(profile.archetype()) && !result.contains("protection")) {
            result.add("protection");
        }
        return result;
    }

    private List<CardResponseDTO> archetypeFallbackCards(CommanderArchetypeProfile profile, String bracket) {
        String archetype = profile == null ? "" : profile.archetype();
        String normalizedBracket = bracket == null ? "casual" : bracket.toLowerCase(Locale.ROOT);
        List<CardResponseDTO> cards = new ArrayList<>();

        if (isCombat(archetype) || "voltron".equals(archetype)) {
            cards.addAll(List.of(
                    card("Utopia Sprawl", "{G}", "Enchantment - Aura", "Enchant Forest. As this Aura enters, choose a color. Whenever enchanted Forest is tapped for mana, its controller adds one mana of the chosen color.", 1.0, "G"),
                    card("Greater Good", "{2}{G}{G}", "Enchantment", "Sacrifice a creature: Draw cards equal to the sacrificed creature's power, then discard three cards.", 4.0, "G"),
                    card("Finale of Devastation", "{X}{G}{G}", "Sorcery", "Search your library and/or graveyard for a creature card with mana value X or less and put it onto the battlefield. If X is 10 or more, creatures you control get +X/+X and gain haste until end of turn.", 2.0, "G"),
                    card("Worldly Tutor", "{G}", "Instant", "Search your library for a creature card, reveal it, then shuffle and put the card on top.", 1.0, "G")
            ));
            if (isCompetitiveBracket(normalizedBracket)) {
                cards.addAll(List.of(
                    card("Pathbreaker Ibex", "{4}{G}{G}", "Creature - Goat", "Whenever this creature attacks, creatures you control gain trample and get +X/+X until end of turn, where X is the greatest power among creatures you control.", 6.0, "G"),
                    card("Scourge of the Throne", "{4}{R}{R}", "Creature - Dragon", "Flying. Dethrone. Whenever this creature attacks for the first time each turn, if it's attacking the player with the most life, untap all attacking creatures. After this phase, there is an additional combat phase.", 6.0, "R"),
                    card("Bloodthirster", "{5}{R}", "Creature - Demon", "Flying, trample. Whenever this creature deals combat damage to a player, untap it. After this combat phase, there is an additional combat phase.", 6.0, "R"),
                    card("Moraug, Fury of Akoum", "{4}{R}{R}", "Legendary Creature - Minotaur Warrior", "Each creature you control gets +1/+0 for each time it has attacked this turn. Landfall - if it's your main phase, there is an additional combat phase after this phase.", 6.0, "R"),
                    card("Old Gnawbone", "{5}{G}{G}", "Legendary Creature - Dragon", "Flying. Whenever a creature you control deals combat damage to a player, create that many Treasure tokens.", 7.0, "G")
                ));
            }
        }

        if ("tokens".equals(archetype)) {
            cards.addAll(List.of(
                    card("Skullclamp", "{1}", "Artifact - Equipment", "Whenever equipped creature dies, draw two cards.", 1.0),
                    card("Tendershoot Dryad", "{4}{G}", "Creature - Dryad", "At the beginning of each upkeep, create a 1/1 Saproling creature token.", 5.0, "G"),
                    card("Beastmaster Ascension", "{2}{G}", "Enchantment", "Creatures you control get +5/+5 if this enchantment has seven or more quest counters.", 3.0, "G")
            ));
        }

        if ("aristocrats".equals(archetype)) {
            cards.addAll(List.of(
                    card("Viscera Seer", "{B}", "Creature - Vampire Wizard", "Sacrifice a creature: Scry 1.", 1.0, "B"),
                    card("Zulaport Cutthroat", "{1}{B}", "Creature - Human Rogue Ally", "Whenever this creature or another creature you control dies, each opponent loses 1 life and you gain 1 life.", 2.0, "B"),
                    card("Pitiless Plunderer", "{3}{B}", "Creature - Human Pirate", "Whenever another creature you control dies, create a Treasure token.", 4.0, "B")
            ));
        }

        if ("reanimator".equals(archetype)) {
            cards.addAll(List.of(
                    card("Reanimate", "{B}", "Sorcery", "Put target creature card from a graveyard onto the battlefield under your control. You lose life equal to its mana value.", 1.0, "B"),
                    card("Animate Dead", "{1}{B}", "Enchantment - Aura", "Enchant creature card in a graveyard. Return it to the battlefield under your control.", 2.0, "B"),
                    card("Entomb", "{B}", "Instant", "Search your library for a card, put that card into your graveyard, then shuffle.", 1.0, "B")
            ));
        }

        if ("spellslinger".equals(archetype)) {
            cards.addAll(List.of(
                    card("Archmage Emeritus", "{2}{U}{U}", "Creature - Human Wizard", "Magecraft - Whenever you cast or copy an instant or sorcery spell, draw a card.", 4.0, "U"),
                    card("Storm-Kiln Artist", "{3}{R}", "Creature - Dwarf Shaman", "Magecraft - Whenever you cast or copy an instant or sorcery spell, create a Treasure token.", 4.0, "R"),
                    card("Jeska's Will", "{2}{R}", "Sorcery", "Add red mana for each card in target opponent's hand. Exile the top three cards of your library. You may play them this turn.", 3.0, "R")
            ));
        }

        if ("control".equals(archetype) || "stax".equals(archetype)) {
            cards.addAll(List.of(
                    card("Esper Sentinel", "{W}", "Artifact Creature - Human Soldier", "Whenever an opponent casts their first noncreature spell each turn, draw a card unless that player pays {X}.", 1.0, "W"),
                    card("Mystic Remora", "{U}", "Enchantment", "Whenever an opponent casts a noncreature spell, you may draw a card unless that player pays {4}.", 1.0, "U"),
                    card("Rhystic Study", "{2}{U}", "Enchantment", "Whenever an opponent casts a spell, you may draw a card unless that player pays {1}.", 3.0, "U"),
                    card("Swords to Plowshares", "{W}", "Instant", "Exile target creature.", 1.0, "W"),
                    card("Swan Song", "{U}", "Instant", "Counter target enchantment, instant, or sorcery spell.", 1.0, "U")
            ));
        }

        if ("turbo-combo".equals(archetype) || "cedh".equals(normalizedBracket)) {
            cards.addAll(List.of(
                    card("Mystic Remora", "{U}", "Enchantment", "Whenever an opponent casts a noncreature spell, you may draw a card unless that player pays {4}.", 1.0, "U"),
                    card("Rhystic Study", "{2}{U}", "Enchantment", "Whenever an opponent casts a spell, you may draw a card unless that player pays {1}.", 3.0, "U"),
                    card("Flusterstorm", "{U}", "Instant", "Counter target instant or sorcery spell unless its controller pays {1}. Storm.", 1.0, "U"),
                    card("Swan Song", "{U}", "Instant", "Counter target enchantment, instant, or sorcery spell.", 1.0, "U"),
                    card("Dark Ritual", "{B}", "Instant", "Add {B}{B}{B}.", 1.0, "B"),
                    card("Thassa's Oracle", "{U}{U}", "Creature - Merfolk Wizard", "When this creature enters, look at the top X cards of your library. If X is greater than or equal to the number of cards in your library, you win the game.", 2.0, "U"),
                    card("Demonic Consultation", "{B}", "Instant", "Name a card. Exile cards from the top of your library until you reveal the named card.", 1.0, "B")
            ));
        }

        return cards;
    }

    private CardResponseDTO strategicPoolCard(String normalizedName) {
        String name = normalizedName == null ? "" : normalizedName.trim().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "savage ventmaw" -> card("Savage Ventmaw", "{4}{R}{G}", "Creature - Dragon", "Flying. Whenever this creature attacks, add {R}{R}{R}{G}{G}{G}. Until end of turn, you don't lose this mana as steps and phases end.", 6.0, "R", "G");
            case "hellkite charger" -> card("Hellkite Charger", "{4}{R}{R}", "Creature - Dragon", "Flying, haste. Whenever this creature attacks, you may pay {5}{R}{R}. If you do, untap all attacking creatures and after this phase, there is an additional combat phase.", 6.0, "R");
            case "bear umbra" -> card("Bear Umbra", "{2}{G}{G}", "Enchantment - Aura", "Enchant creature. Whenever enchanted creature attacks, untap all lands you control.", 4.0, "G");
            case "brain freeze" -> card("Brain Freeze", "{1}{U}", "Instant", "Target player mills three cards. Storm.", 2.0, "U");
            case "thassa's oracle" -> card("Thassa's Oracle", "{U}{U}", "Creature - Merfolk Wizard", "When this creature enters, look at the top X cards of your library. If X is greater than or equal to the number of cards in your library, you win the game.", 2.0, "U");
            case "demonic consultation" -> card("Demonic Consultation", "{B}", "Instant", "Name a card. Exile cards from the top of your library until you reveal the named card.", 1.0, "B");
            default -> null;
        };
    }

    private List<CardResponseDTO> fallbackCards(String role, String bracket) {
        List<CardResponseDTO> bracketCards = bracketFallbackCards(role, bracket);
        if (!bracketCards.isEmpty()) {
            return bracketCards;
        }
        return switch (role) {
            case "ramp", "curve" -> List.of(
                    card("Nature's Lore", "{1}{G}", "Sorcery", "Search your library for a Forest card and put it onto the battlefield.", 2.0, "G"),
                    card("Farseek", "{1}{G}", "Sorcery", "Search your library for a Plains, Island, Swamp, or Mountain card and put it onto the battlefield tapped.", 2.0, "G"),
                    card("Arcane Signet", "{2}", "Artifact", "Add one mana of any color in your commander's color identity.", 2.0),
                    card("Fellwar Stone", "{2}", "Artifact", "Add one mana of any color that a land an opponent controls could produce.", 2.0),
                    card("Marble Diamond", "{2}", "Artifact", "Marble Diamond enters the battlefield tapped. Add {W}.", 2.0),
                    card("Sky Diamond", "{2}", "Artifact", "Sky Diamond enters the battlefield tapped. Add {U}.", 2.0)
            );
            case "draw" -> List.of(
                    card("Greater Good", "{2}{G}{G}", "Enchantment", "Sacrifice a creature: Draw cards equal to the sacrificed creature's power, then discard three cards.", 4.0, "G"),
                    card("Harmonize", "{2}{G}{G}", "Sorcery", "Draw three cards.", 4.0, "G"),
                    card("Village Rites", "{B}", "Instant", "As an additional cost to cast this spell, sacrifice a creature. Draw two cards.", 1.0, "B"),
                    card("Fact or Fiction", "{3}{U}", "Instant", "Reveal the top five cards of your library. An opponent separates those cards into two piles. Put one pile into your hand.", 4.0, "U"),
                    card("Windfall", "{2}{U}", "Sorcery", "Each player discards their hand, then draws cards equal to the greatest number of cards a player discarded this way.", 3.0, "U"),
                    card("Archivist", "{2}{U}{U}", "Creature - Human Wizard", "{T}: Draw a card.", 4.0, "U")
            );
            case "removal" -> List.of(
                    card("Beast Within", "{2}{G}", "Instant", "Destroy target permanent.", 3.0, "G"),
                    card("Generous Gift", "{2}{W}", "Instant", "Destroy target permanent.", 3.0, "W"),
                    card("Chaos Warp", "{2}{R}", "Instant", "The owner of target permanent shuffles it into their library.", 3.0, "R"),
                    card("Reality Shift", "{1}{U}", "Instant", "Exile target creature.", 2.0, "U"),
                    card("Pongify", "{U}", "Instant", "Destroy target creature. It can't be regenerated.", 1.0, "U"),
                    card("Resculpt", "{1}{U}", "Instant", "Exile target artifact or creature.", 2.0, "U")
            );
            case "protection" -> List.of(
                    card("Heroic Intervention", "{1}{G}", "Instant", "Permanents you control gain hexproof and indestructible until end of turn.", 2.0, "G"),
                    card("Swiftfoot Boots", "{2}", "Artifact - Equipment", "Equipped creature has hexproof and haste.", 2.0),
                    card("Boros Charm", "{R}{W}", "Instant", "Permanents you control gain indestructible until end of turn.", 2.0, "R", "W"),
                    card("Flawless Maneuver", "{2}{W}", "Instant", "Creatures you control gain indestructible until end of turn.", 3.0, "W"),
                    card("Loran's Escape", "{W}", "Instant", "Target artifact or creature gains hexproof and indestructible until end of turn.", 1.0, "W"),
                    card("Slip Out the Back", "{U}", "Instant", "Put a +1/+1 counter on target creature. It phases out.", 1.0, "U")
            );
            case "finisher" -> List.of(
                    card("Overwhelming Stampede", "{3}{G}{G}", "Sorcery", "Creatures you control gain trample and get +X/+X until end of turn.", 5.0, "G"),
                    card("Craterhoof Behemoth", "{5}{G}{G}{G}", "Creature - Beast", "When this creature enters, creatures you control gain trample and get +X/+X until end of turn.", 8.0, "G"),
                    card("Approach of the Second Sun", "{6}{W}", "Sorcery", "If this spell was cast from your hand and you've cast another spell named Approach of the Second Sun this game, you win the game.", 7.0, "W"),
                    card("Shark Typhoon", "{5}{U}", "Enchantment", "Whenever you cast a noncreature spell, create an X/X blue Shark creature token with flying.", 6.0, "U")
            );
            case "land" -> List.of(
                    card("Path of Ancestry", "", "Land", "Add one mana of any color in your commander's color identity.", 0.0),
                    card("Evolving Wilds", "", "Land", "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.", 0.0),
                    card("Terramorphic Expanse", "", "Land", "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.", 0.0)
            );
            default -> List.of();
        };
    }

    private List<CardResponseDTO> bracketFallbackCards(String role, String bracket) {
        String normalizedBracket = bracket == null ? "casual" : bracket.toLowerCase(Locale.ROOT);
        if (!"cedh".equals(normalizedBracket) && !"high-power".equals(normalizedBracket)) {
            return List.of();
        }
        return switch (role) {
            case "ramp", "curve" -> List.of(
                    card("Sol Ring", "{1}", "Artifact", "{T}: Add {C}{C}.", 1.0),
                    card("Arcane Signet", "{2}", "Artifact", "Add one mana of any color in your commander's color identity.", 2.0),
                    card("Dark Ritual", "{B}", "Instant", "Add {B}{B}{B}.", 1.0, "B")
            );
            case "draw" -> List.of(
                    card("Mystic Remora", "{U}", "Enchantment", "Whenever an opponent casts a noncreature spell, you may draw a card unless that player pays {4}.", 1.0, "U"),
                    card("Rhystic Study", "{2}{U}", "Enchantment", "Whenever an opponent casts a spell, you may draw a card unless that player pays {1}.", 3.0, "U")
            );
            case "removal" -> List.of(
                    card("Flusterstorm", "{U}", "Instant", "Counter target instant or sorcery spell unless its controller pays {1}. Storm.", 1.0, "U"),
                    card("Swan Song", "{U}", "Instant", "Counter target enchantment, instant, or sorcery spell.", 1.0, "U"),
                    card("Nature's Claim", "{G}", "Instant", "Destroy target artifact or enchantment.", 1.0, "G")
            );
            case "protection" -> List.of(
                    card("Veil of Summer", "{G}", "Instant", "Draw a card if an opponent has cast a blue or black spell this turn. Spells you control can't be countered this turn.", 1.0, "G"),
                    card("Silence", "{W}", "Instant", "Your opponents can't cast spells this turn.", 1.0, "W"),
                    card("Flusterstorm", "{U}", "Instant", "Counter target instant or sorcery spell unless its controller pays {1}. Storm.", 1.0, "U")
            );
            case "finisher" -> List.of(
                    card("Thassa's Oracle", "{U}{U}", "Creature - Merfolk Wizard", "When this creature enters, look at the top X cards of your library. If X is greater than or equal to the number of cards in your library, you win the game.", 2.0, "U"),
                    card("Demonic Consultation", "{B}", "Instant", "Name a card. Exile the top six cards of your library, then reveal cards until you reveal the named card.", 1.0, "B")
            );
            default -> List.of();
        };
    }

    private CardResponseDTO card(String name, String manaCost, String typeLine, String oracle, Double cmc, String... colors) {
        return new CardResponseDTO(name, manaCost, typeLine, oracle, cmc, List.of(colors), List.of());
    }

    private boolean isLegalAdd(CardResponseDTO card, Set<String> existingNames, Set<String> commanderColors) {
        if (card == null || card.name() == null) return false;
        String normalizedName = normalize(card.name());
        if (existingNames.contains(normalizedName)) return false;
        if (commanderBanlist().isBanned(card.name())) return false;
        if (!ColorIdentityMatcher.matches(card, commanderColors)) return false;
        String typeLine = card.typeLine() == null ? "" : card.typeLine().toLowerCase(Locale.ROOT);
        return !typeLine.contains("plane") && !typeLine.contains("scheme") && !typeLine.contains("conspiracy");
    }

    private CommanderBanlistService commanderBanlist() {
        if (commanderBanlistService == null) {
            commanderBanlistService = new CommanderBanlistService();
        }
        return commanderBanlistService;
    }

    private String classifyRole(CardResponseDTO card) {
        return classifier().primaryRole(card);
    }

    private CardRoleClassifier classifier() {
        return roleClassifier == null ? new CardRoleClassifier() : roleClassifier;
    }

    private boolean isCompetitiveBracket(String bracket) {
        return "cedh".equalsIgnoreCase(bracket) || "high-power".equalsIgnoreCase(bracket);
    }

    private boolean isCombat(String archetype) {
        return "combat".equals(archetype) || "combat damage".equals(archetype);
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
        if (isCombat(profile.archetype()) && (oracle.contains("trample") || oracle.contains("combat") || oracle.contains("double strike"))) return 1.0;
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
            case "mid" -> synergy * 0.30 + gapFix * 0.25 + meta * 0.25 + archetypeFit * 0.10 + efficiency * 0.10;
            default -> synergy * 0.35 + gapFix * 0.25 + archetypeFit * 0.20 + efficiency * 0.10 + Math.min(meta, bracketFit) * 0.10;
        };
    }

    private boolean isStackInteraction(CardResponseDTO card) {
        String oracle = text(card.oracleText());
        return oracle.contains("counter target") || oracle.contains("you may cast") || oracle.contains("mana value 1 or less");
    }

    private String addReason(String role, CommanderArchetypeProfile profile, StrategicDeckAssessment assessment) {
        String structuralContext = assessment == null ? "" : " (" + assessment.primaryIssueForRole(role) + ")";
        return switch (role) {
            case "draw" -> "aumenta card advantage sem fugir do plano " + profile.archetype();
            case "ramp" -> "melhora a curva inicial e acelera o plano " + profile.archetype();
            case "removal" -> "aumenta interação para proteger o plano principal";
            case "protection" -> "protege comandante ou peças-chave do plano";
            case "combo-piece" -> "aumenta redundancia de combo e inevitabilidade no plano " + profile.archetype();
            case "tutor" -> "encontra a ameaca, resposta ou peca de combo certa com menos variancia";
            case "finisher" -> "converte recursos acumulados em condição de vitória";
            default -> "aumenta consistência e sinergia geral";
        } + structuralContext;
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
