package com.mtg.service;

import com.mtg.domain.StrategicRecommendation;
import com.mtg.dto.CardResponseDTO;
import com.mtg.model.Deck;
import com.mtg.service.meta.MetaCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class StrategicRecommendationEngine {
    @Inject DeckRoleAnalyzer deckRoleAnalyzer;
    @Inject CommanderArchetypeDetector archetypeDetector;
    @Inject CandidateAddSelector addSelector;
    @Inject CandidateCutSelector cutSelector;
    @Inject RecommendationPairer pairer;
    @Inject StrategicDeckAnalyzer strategicDeckAnalyzer;

    public Result recommend(Scenario scenario) {
        CardResponseDTO commanderCard = scenario.knownCards().get(normalize(scenario.deck().getCommander()));
        DeckRoleSummary roles = deckRoleAnalyzer.analyze(scenario.deck(), scenario.knownCards(), scenario.bracket());
        CommanderArchetypeProfile profile = archetypeDetector.detect(
                scenario.deck().getCommander(),
                commanderCard,
                roles,
                scenario.persistedColors()
        );
        StrategicDeckAssessment assessment = strategicDeckAnalyzer.assess(
                scenario.deck(),
                scenario.knownCards(),
                profile,
                roles,
                scenario.bracket()
        );
        List<StrategicCandidate> adds = addSelector.select(
                scenario.deck(),
                scenario.metaCards(),
                new HashMap<>(scenario.knownCards()),
                profile,
                roles,
                scenario.bracket(),
                scenario.hasUsefulMeta(),
                scenario.recommendationMode(),
                scenario.budget(),
                scenario.filters(),
                assessment,
                scenario.ownedCardNames()
        );
        List<StrategicCandidate> cuts = cutSelector.select(
                scenario.deck(),
                scenario.knownCards(),
                profile,
                roles,
                scenario.bracket(),
                assessment
        );
        List<StrategicRecommendation> recommendations = pairer.pair(
                adds,
                cuts,
                profile,
                roles,
                scenario.maxRecommendations(),
                scenario.bracket(),
                scenario.metaSampleSize(),
                scenario.sourcesUsed(),
                scenario.recommendationMode(),
                scenario.budget(),
                scenario.currentGameChangers()
        );
        return new Result(recommendations, profile, roles, assessment, adds.size(), cuts.size());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record Scenario(
            Deck deck,
            List<MetaCard> metaCards,
            Map<String, CardResponseDTO> knownCards,
            Set<String> persistedColors,
            String bracket,
            boolean hasUsefulMeta,
            String recommendationMode,
            Double budget,
            Set<String> filters,
            Set<String> ownedCardNames,
            int maxRecommendations,
            int metaSampleSize,
            List<String> sourcesUsed,
            int currentGameChangers
    ) {
        public Scenario {
            metaCards = metaCards == null ? List.of() : List.copyOf(metaCards);
            knownCards = knownCards == null ? Map.of() : Map.copyOf(knownCards);
            persistedColors = persistedColors == null ? Set.of() : Set.copyOf(persistedColors);
            filters = filters == null ? Set.of() : Set.copyOf(filters);
            ownedCardNames = ownedCardNames == null ? Set.of() : Set.copyOf(ownedCardNames);
            sourcesUsed = sourcesUsed == null ? List.of() : List.copyOf(sourcesUsed);
        }
    }

    public record Result(
            List<StrategicRecommendation> recommendations,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            StrategicDeckAssessment assessment,
            int addCandidates,
            int cutCandidates
    ) {
        public Result {
            recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        }
    }
}
