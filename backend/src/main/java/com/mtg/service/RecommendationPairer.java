package com.mtg.service;

import com.mtg.domain.StrategicRecommendation;
import com.mtg.domain.MetaComparison;
import com.mtg.domain.RecommendationCardInsight;
import com.mtg.domain.RecommendationImpact;
import com.mtg.domain.RecommendationSourceContext;
import com.mtg.service.meta.RoleTargets;
import com.mtg.service.rules.CommanderGameChangerService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class RecommendationPairer {

    @Inject
    RecommendationReasoningBuilder reasoningBuilder;

    @Inject
    CommanderGameChangerService commanderGameChangerService;

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
        return pair(adds, cuts, profile, roles, maxRecommendations, bracket, sampleSize, sources, recommendationMode, budget, 0);
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
            Double budget,
            int currentGameChangers
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
            RecommendationImpact impact = impactFor(add, cut, roles, currentGameChangers);
            recommendations.add(new StrategicRecommendation(
                    recommendationId(add, cut),
                    reasoningBuilder.build(add, cut, profile, roles),
                    problemFor(add, roles, bracket, profile),
                    add.card().name(),
                    cut.card().name(),
                    riskFor(add, cut),
                    numericImpact(impact),
                    tagsFor(add),
                    source,
                    bracket,
                    confidenceFor(add, sampleSize, source),
                    "swap",
                    new RecommendationSourceContext(source, sampleSize, sources),
                    impact,
                    insightFor(add, sampleSize, source),
                    insightFor(cut, 0, "deck_current"),
                    comparisonsFor(add, roles, bracket, profile),
                    List.of(),
                    recommendationMode,
                    budget
            ));
        }

        return recommendations;
    }

    private String recommendationId(StrategicCandidate add, StrategicCandidate cut) {
        return normalize(add.card().name()) + "__" + normalize(cut.card().name());
    }

    private String problemFor(StrategicCandidate add, DeckRoleSummary roles, String bracket, CommanderArchetypeProfile profile) {
        String role = add.role();
        int current = countForRole(role, roles);
        int target = targetForRole(role, bracket, profile);
        if (target > 0 && current < target) {
            return "O deck esta abaixo do alvo de " + roleLabel(role) + " para este bracket: "
                    + current + " atual vs " + target + " recomendado.";
        }
        if ("ramp".equals(role)) {
            return "A base de mana e a curva podem ficar mais consistentes com uma fonte de mana melhor.";
        }
        if ("removal".equals(role) || "protection".equals(role)) {
            return "O deck pode aumentar a seguranca do plano principal com mais interacao ou protecao.";
        }
        return "Existe uma troca incremental para melhorar sinergia, eficiencia ou aderencia ao plano do comandante.";
    }

    private String riskFor(StrategicCandidate add, StrategicCandidate cut) {
        double addCmc = add.card().cmc() == null ? 0.0 : add.card().cmc();
        double cutCmc = cut.card().cmc() == null ? 0.0 : cut.card().cmc();
        if (addCmc > cutCmc + 2.0) {
            return "Risco medio: a troca aumenta a curva e pode deixar maos iniciais mais lentas.";
        }
        if (!add.role().equals(cut.role())) {
            return "Risco medio: a troca muda a distribuicao de papeis; confira se o tema nao perde uma peca importante.";
        }
        return "Risco baixo: troca direta no mesmo papel funcional e pode ser desfeita pelo historico.";
    }

    private Map<String, Double> numericImpact(RecommendationImpact impact) {
        if (impact == null) {
            return Map.of();
        }
        return Map.of(
                "averageCmcDelta", round(impact.averageCmcAfter() - impact.averageCmcBefore()),
                "rampDelta", (double) (impact.rampAfter() - impact.rampBefore()),
                "drawDelta", (double) (impact.drawAfter() - impact.drawBefore()),
                "interactionDelta", (double) (impact.removalAfter() - impact.removalBefore()),
                "protectionDelta", (double) (impact.protectionAfter() - impact.protectionBefore()),
                "gameChangersDelta", (double) (impact.gameChangersAfter() - impact.gameChangersBefore()),
                "bracketPressureDelta", (double) (impact.bracketPressureAfter() - impact.bracketPressureBefore())
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
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

    private String confidenceFor(StrategicCandidate add, int sampleSize, String source) {
        if (add.metaDriven() && sampleSize >= 10 && add.inclusionRate() >= 0.60) {
            return "high";
        }
        if (add.metaDriven() && sampleSize >= 3) {
            return add.score() >= 0.45 ? "medium" : "low";
        }
        if ("heuristic_fallback".equals(source) && add.score() < 0.68) {
            return "low";
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

    private RecommendationImpact impactFor(StrategicCandidate add, StrategicCandidate cut, DeckRoleSummary roles, int currentGameChangers) {
        String role = add.role();
        int rampAfter = nextRoleCount(roles.ramp(), role, cut.role(), "ramp");
        int drawAfter = nextRoleCount(roles.draw(), role, cut.role(), "draw");
        int removalAfter = nextRoleCount(roles.removal(), role, cut.role(), "removal");
        int protectionAfter = nextRoleCount(roles.protection(), role, cut.role(), "protection");
        double averageCmcAfter = averageAfterSwap(roles.averageCmc(), roles.totalCards(), cut.card().cmc(), add.card().cmc());
        int gameChangersAfter = Math.max(0, currentGameChangers - gameChangerValue(cut.card()) + gameChangerValue(add.card()));
        return new RecommendationImpact(
                role,
                roles.averageCmc(),
                averageCmcAfter,
                roles.ramp(),
                rampAfter,
                roles.draw(),
                drawAfter,
                roles.removal(),
                removalAfter,
                roles.protection(),
                protectionAfter,
                currentGameChangers,
                gameChangersAfter,
                bracketPressure(roles.averageCmc(), roles.ramp(), roles.draw(), roles.removal(), roles.protection(), roles.finishers(), currentGameChangers),
                bracketPressure(averageCmcAfter, rampAfter, drawAfter, removalAfter, protectionAfter, roles.finishers(), gameChangersAfter)
        );
    }

    private int gameChangerValue(com.mtg.dto.CardResponseDTO card) {
        if (card == null || card.name() == null || commanderGameChangerService == null) {
            return 0;
        }
        return commanderGameChangerService.isGameChanger(card.name()) ? 1 : 0;
    }

    private int bracketPressure(
            double averageCmc,
            int ramp,
            int draw,
            int removal,
            int protection,
            int finishers,
            int gameChangers
    ) {
        int speed = clampScore((int) Math.round((4.2 - averageCmc) * 18 + ramp * 2.2));
        int consistency = clampScore((int) Math.round(draw * 5.0 + ramp * 1.4));
        int interaction = clampScore(removal * 7 + protection * 5);
        int threat = clampScore(finishers * 12 + gameChangers * 16);
        return clampScore((speed + consistency + interaction + threat) / 4);
    }

    private int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
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

    private List<MetaComparison> comparisonsFor(StrategicCandidate add, DeckRoleSummary roles, String bracket, CommanderArchetypeProfile profile) {
        String role = add.role();
        int current = countForRole(role, roles);
        int target = targetForRole(role, bracket, profile);
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

    private int targetForRole(String role, String bracket, CommanderArchetypeProfile profile) {
        RoleTargets targets = RoleTargets.forBracket(bracket);
        int target = switch (role == null ? "" : role) {
            case "ramp" -> targets.ramp();
            case "draw" -> targets.draw();
            case "removal" -> targets.removal();
            case "protection" -> targets.protection();
            case "finisher" -> 4;
            default -> 0;
        };
        String archetype = profile == null ? "" : profile.archetype();
        if ("combat".equals(archetype) || "combat damage".equals(archetype) || "midrange".equals(archetype) || "ramp".equals(archetype)) {
            if ("ramp".equals(role) || "protection".equals(role)) target++;
            if ("finisher".equals(role)) target += 2;
        }
        if ("aristocrats".equals(archetype) || "tokens".equals(archetype)) {
            if ("draw".equals(role)) target++;
            if ("finisher".equals(role)) target = Math.max(3, target - 1);
        }
        if ("control".equals(archetype) && "removal".equals(role)) {
            target += 2;
        }
        return target;
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
