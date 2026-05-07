package com.mtg.service;

import com.mtg.domain.StrategicRecommendation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class RecommendationPairer {

    @Inject
    RecommendationReasoningBuilder reasoningBuilder;

    public List<StrategicRecommendation> pair(
            List<StrategicCandidate> adds,
            List<StrategicCandidate> cuts,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles
    ) {
        return pair(adds, cuts, profile, roles, 5);
    }

    public List<StrategicRecommendation> pair(
            List<StrategicCandidate> adds,
            List<StrategicCandidate> cuts,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            int maxRecommendations
    ) {
        return pair(adds, cuts, profile, roles, maxRecommendations, "casual");
    }

    public List<StrategicRecommendation> pair(
            List<StrategicCandidate> adds,
            List<StrategicCandidate> cuts,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            int maxRecommendations,
            String bracket
    ) {
        List<StrategicRecommendation> recommendations = new ArrayList<>();
        Set<String> usedAdds = new HashSet<>();
        Set<String> usedCuts = new HashSet<>();

        for (StrategicCandidate add : adds) {
            if (recommendations.size() >= maxRecommendations) {
                break;
            }
            if (!usedAdds.add(normalize(add.card().name()))) {
                continue;
            }

            StrategicCandidate cut = bestCutFor(add, cuts, usedCuts);
            if (cut == null) {
                continue;
            }
            usedCuts.add(normalize(cut.card().name()));
            recommendations.add(new StrategicRecommendation(
                    reasoningBuilder.build(add, cut, profile, roles),
                    add.card().name(),
                    cut.card().name(),
                    tagsFor(add),
                    add.metaDriven() ? "meta_profile" : "heuristic_fallback",
                    bracket,
                    confidenceFor(add)
            ));
        }

        return recommendations;
    }

    private List<String> tagsFor(StrategicCandidate add) {
        List<String> tags = new ArrayList<>();
        if (add.metaDriven()) {
            tags.add("meta");
        } else {
            tags.add("fallback");
        }
        if (add.role() != null && !add.role().isBlank()) {
            tags.add(add.role());
        }
        if (add.score() >= 0.72) {
            tags.add("efficiency");
        }
        tags.add("synergy");
        return tags.stream().distinct().toList();
    }

    private String confidenceFor(StrategicCandidate add) {
        if (add.metaDriven() && add.inclusionRate() >= 0.70) {
            return "high";
        }
        if (add.score() >= 0.55) {
            return "medium";
        }
        return "low";
    }

    private StrategicCandidate bestCutFor(StrategicCandidate add, List<StrategicCandidate> cuts, Set<String> usedCuts) {
        StrategicCandidate fallback = null;
        for (StrategicCandidate cut : cuts) {
            String cutName = normalize(cut.card().name());
            if (usedCuts.contains(cutName)) {
                continue;
            }
            if (add.role().equals(cut.role())) {
                return cut;
            }
            if (fallback == null) {
                fallback = cut;
            }
        }
        return fallback;
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }
}
