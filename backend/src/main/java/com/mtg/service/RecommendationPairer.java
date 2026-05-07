package com.mtg.service;

import com.mtg.domain.StrategicRecommendation;
import com.mtg.domain.MetaComparison;
import com.mtg.domain.RecommendationCardInsight;
import com.mtg.domain.RecommendationImpact;
import com.mtg.domain.RecommendationSourceContext;
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
        return pair(adds, cuts, profile, roles, maxRecommendations, bracket, 0, List.of(), "consistency", null);
    }

    public List<StrategicRecommendation> pair(
            List<StrategicCandidate> adds,
            List<StrategicCandidate> cuts,
            CommanderArchetypeProfile profile,
            DeckRoleSummary roles,
            int maxRecommendations,
            String bracket,
            int sampleSize,
            List<String> sources,
            String recommendationMode,
            Double budget
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
            String source = add.metaDriven() ? "meta_profile" : "heuristic_fallback";
            recommendations.add(new StrategicRecommendation(
                    reasoningBuilder.build(add, cut, profile, roles),
                    add.card().name(),
                    cut.card().name(),
                    tagsFor(add),
                    source,
                    bracket,
                    confidenceFor(add),
                    "swap",
                    new RecommendationSourceContext(source, sampleSize, sources),
                    impactFor(add, cut, roles),
                    insightFor(add, sampleSize, source),
                    insightFor(cut, 0, "deck_current"),
                    comparisonsFor(add, roles, bracket),
                    List.of(),
                    recommendationMode,
                    budget
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

    private RecommendationCardInsight insightFor(StrategicCandidate candidate, int sampleSize, String source) {
        return new RecommendationCardInsight(
                candidate.card().name(),
                candidate.role(),
                candidate.inclusionRate(),
                sampleSize,
                candidate.synergyEstimate(),
                sourceFor(candidate, source),
                candidate.card().estimatedPrice(),
                candidate.card().estimatedPrice() == null ? null : "Preco estimado via Scryfall; pode estar defasado conforme mercado, idioma, edicao e disponibilidade."
        );
    }

    private String sourceFor(StrategicCandidate candidate, String fallback) {
        if (candidate.source() != null && !candidate.source().isBlank()) {
            return candidate.source();
        }
        return fallback;
    }

    private RecommendationImpact impactFor(StrategicCandidate add, StrategicCandidate cut, DeckRoleSummary roles) {
        String role = add.role();
        return new RecommendationImpact(
                role,
                roles.averageCmc(),
                averageAfterSwap(roles.averageCmc(), roles.totalCards(), cut.card().cmc(), add.card().cmc()),
                roles.ramp(),
                nextRoleCount(roles.ramp(), role, cut.role(), "ramp"),
                roles.draw(),
                nextRoleCount(roles.draw(), role, cut.role(), "draw"),
                roles.removal(),
                nextRoleCount(roles.removal(), role, cut.role(), "removal"),
                roles.protection(),
                nextRoleCount(roles.protection(), role, cut.role(), "protection")
        );
    }

    private Double averageAfterSwap(double averageBefore, int totalCards, Double cutCmc, Double addCmc) {
        if (totalCards <= 0) {
            return averageBefore;
        }
        double beforeSum = averageBefore * totalCards;
        double afterSum = beforeSum - (cutCmc == null ? 0.0 : cutCmc) + (addCmc == null ? 0.0 : addCmc);
        return afterSum / totalCards;
    }

    private Integer nextRoleCount(int current, String addRole, String cutRole, String trackedRole) {
        int next = current;
        if (trackedRole.equals(addRole)) {
            next++;
        }
        if (trackedRole.equals(cutRole)) {
            next--;
        }
        return Math.max(0, next);
    }

    private List<MetaComparison> comparisonsFor(StrategicCandidate add, DeckRoleSummary roles, String bracket) {
        String role = add.role();
        int current = countForRole(role, roles);
        int target = targetForRole(role, bracket);
        if (target <= 0 || current >= target) {
            return List.of();
        }

        int percentileBehind = Math.min(95, Math.max(55, 50 + (target - current) * 7));
        String label = roleLabel(role);
        String message = "Seu deck tem menos " + label + " que aproximadamente " + percentileBehind
                + "% das listas similares neste bracket: " + current + " no deck vs alvo " + target + ".";
        return List.of(new MetaComparison(role, current, target, percentileBehind, message));
    }

    private int countForRole(String role, DeckRoleSummary roles) {
        return switch (role == null ? "" : role) {
            case "ramp" -> roles.ramp();
            case "draw" -> roles.draw();
            case "removal" -> roles.removal();
            case "protection" -> roles.protection();
            case "finisher" -> roles.finishers();
            default -> 0;
        };
    }

    private int targetForRole(String role, String bracket) {
        String normalizedBracket = bracket == null ? "casual" : bracket.toLowerCase();
        return switch (role == null ? "" : role) {
            case "ramp" -> switch (normalizedBracket) {
                case "cedh" -> 14;
                case "high-power" -> 12;
                case "mid" -> 10;
                default -> 9;
            };
            case "draw" -> switch (normalizedBracket) {
                case "cedh" -> 12;
                case "high-power" -> 10;
                case "mid" -> 9;
                default -> 8;
            };
            case "removal" -> switch (normalizedBracket) {
                case "cedh" -> 14;
                case "high-power" -> 10;
                case "mid" -> 8;
                default -> 7;
            };
            case "protection" -> "high-power".equals(normalizedBracket) || "cedh".equals(normalizedBracket) ? 4 : 2;
            case "finisher" -> 4;
            default -> 0;
        };
    }

    private String roleLabel(String role) {
        return switch (role == null ? "" : role) {
            case "ramp" -> "ramp";
            case "draw" -> "compra";
            case "removal" -> "interacao";
            case "protection" -> "protecao";
            case "finisher" -> "condicoes de vitoria";
            default -> "cartas deste papel";
        };
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
