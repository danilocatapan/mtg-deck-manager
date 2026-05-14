package com.mtg.service;

import com.mtg.dto.CardResponseDTO;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record StrategicDeckAssessment(
        Set<String> issues,
        Map<String, Double> rolePriorities,
        Set<String> weakCards
) {
    public StrategicDeckAssessment {
        issues = issues == null ? Set.of() : Set.copyOf(issues);
        rolePriorities = rolePriorities == null ? Map.of() : Map.copyOf(rolePriorities);
        weakCards = weakCards == null ? Set.of() : Set.copyOf(weakCards);
    }

    public static StrategicDeckAssessment empty() {
        return new StrategicDeckAssessment(Set.of(), Map.of(), Set.of());
    }

    public boolean hasIssue(String issue) {
        return issues.contains(issue);
    }

    public double priorityFor(String role) {
        return rolePriorities.getOrDefault(role, 1.0);
    }

    public boolean isWeakCard(String cardName) {
        return weakCards.contains(normalize(cardName));
    }

    public String primaryIssueForRole(String role) {
        if ("ramp".equals(role) && hasIssue("slow-ramp")) return "o ramp atual esta lento para o bracket";
        if ("draw".equals(role) && hasIssue("conditional-draw")) return "a compra atual depende demais de conectar ou de alto poder";
        if ("removal".equals(role) && hasIssue("low-interaction")) return "o deck precisa de interacao mais eficiente";
        if ("protection".equals(role) && hasIssue("low-protection")) return "o plano principal precisa de mais protecao";
        if (("finisher".equals(role) || "combo-piece".equals(role)) && hasIssue("low-inevitability")) {
            return "o deck precisa transformar vantagem em fechamento real de mesa";
        }
        if (("tutor".equals(role) || "selection".equals(role)) && hasIssue("low-consistency")) {
            return "o deck precisa encontrar as pecas certas com mais consistencia";
        }
        if ("land".equals(role) && hasIssue("tapped-lands")) return "a base de mana tem terrenos lentos";
        if (hasIssue("high-curve")) return "a curva media esta pressionando a velocidade do deck";
        return "essa troca ataca uma fraqueza estrutural do deck";
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static class Builder {
        private final Set<String> issues = new LinkedHashSet<>();
        private final Map<String, Double> rolePriorities = new LinkedHashMap<>();
        private final Set<String> weakCards = new LinkedHashSet<>();

        public Builder issue(String issue) {
            if (issue != null && !issue.isBlank()) {
                issues.add(issue);
            }
            return this;
        }

        public Builder weakCard(String cardName) {
            if (cardName != null && !cardName.isBlank()) {
                weakCards.add(normalize(cardName));
            }
            return this;
        }

        public Builder priority(String role, double value) {
            if (role != null && !role.isBlank()) {
                rolePriorities.merge(role, value, Math::max);
            }
            return this;
        }

        public StrategicDeckAssessment build() {
            return new StrategicDeckAssessment(issues, rolePriorities, weakCards);
        }
    }
}
