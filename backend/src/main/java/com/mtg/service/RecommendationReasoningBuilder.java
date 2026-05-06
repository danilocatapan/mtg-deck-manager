package com.mtg.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecommendationReasoningBuilder {

    public String build(StrategicCandidate add, StrategicCandidate cut, CommanderArchetypeProfile profile, DeckRoleSummary roles) {
        String problem = strategicProblem(add.role(), profile, roles);
        String addSentence = add.card().name() + " melhora esse ponto porque " + add.reason()
                + " e encaixa no plano de " + profile.plan() + ".";
        String cutSentence = cut.card().name() + " é o corte mais adequado porque " + cut.reason()
                + ", então a troca aumenta a qualidade média sem transformar o deck em uma lista genérica.";
        return problem + " " + addSentence + " " + cutSentence;
    }

    private String strategicProblem(String role, CommanderArchetypeProfile profile, DeckRoleSummary roles) {
        return switch (role) {
            case "draw" -> "O deck precisa manter fluxo de cartas para sustentar o plano " + profile.archetype()
                    + ", especialmente depois das primeiras trocas de recursos.";
            case "ramp" -> "O deck quer executar seu plano antes que a mesa estabilize, e a curva média de "
                    + String.format(java.util.Locale.ROOT, "%.2f", roles.averageCmc()) + " recompensa ramp mais eficiente.";
            case "removal" -> "O deck precisa de interação que responda ameaças sem perder o foco no plano do comandante.";
            case "protection" -> "A estratégia depende de comandante ou peças-chave sobreviverem por turnos suficientes para gerar vantagem real.";
            case "finisher" -> "O deck precisa converter recursos acumulados em uma forma clara de encerrar a partida.";
            default -> "A lista ganha mais consistência quando slots genéricos viram peças alinhadas ao plano do comandante.";
        };
    }
}
