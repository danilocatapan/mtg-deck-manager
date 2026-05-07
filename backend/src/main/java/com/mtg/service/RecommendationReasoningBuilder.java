package com.mtg.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecommendationReasoningBuilder {

    public String build(StrategicCandidate add, StrategicCandidate cut, CommanderArchetypeProfile profile, DeckRoleSummary roles) {
        String priority = strategicPriority(add.role(), roles);
        String metaContext = add.metaDriven()
                ? " Em listas similares de " + profile.commanderName()
                + ", essa carta aparece com frequencia relevante ("
                + String.format(java.util.Locale.ROOT, "%.0f%%", add.inclusionRate() * 100.0)
                + "), reforcando o encaixe no plano."
                : "";
        return priority + " " + add.card().name() + " melhora esse ponto porque " + add.reason()
                + "." + metaContext + " " + cut.card().name() + " e um corte melhor porque "
                + cut.reason() + ".";
    }

    private String strategicPriority(String role, DeckRoleSummary roles) {
        return switch (role) {
            case "draw" -> "Prioridade: manter fluxo de cartas.";
            case "ramp" -> "Prioridade: acelerar uma curva media de "
                    + String.format(java.util.Locale.ROOT, "%.2f", roles.averageCmc()) + ".";
            case "removal" -> "Prioridade: melhorar interacao sem perder foco.";
            case "protection" -> "Prioridade: proteger comandante ou pecas-chave.";
            case "finisher" -> "Prioridade: converter recursos em condicao de vitoria.";
            default -> "Prioridade: substituir um slot generico por uma peca mais alinhada.";
        };
    }
}
