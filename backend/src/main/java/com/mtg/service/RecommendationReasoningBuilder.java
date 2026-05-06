package com.mtg.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecommendationReasoningBuilder {

    public String build(StrategicCandidate add, StrategicCandidate cut, CommanderArchetypeProfile profile, DeckRoleSummary roles) {
        String priority = strategicPriority(add.role(), roles);
        return "Troque " + cut.card().name() + " por " + add.card().name() + ". "
                + priority + " A adicao " + add.reason() + "; o corte " + cut.reason()
                + ". Resultado: mais foco no plano " + profile.archetype() + ".";
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
