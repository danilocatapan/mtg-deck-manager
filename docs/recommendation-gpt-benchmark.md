# Benchmark: recomendador Commander vs GPT

Ultima atualizacao: 2026-06-03

## Objetivo

Definir quando o MTG Deck Manager pode afirmar que suas recomendacoes de upgrade Commander sao melhores que um prompt generico de GPT para o mesmo deck e usuario.

O sistema so deve afirmar superioridade quando houver evidencia no benchmark e a execucao atual estiver com `medium_confidence` ou `high_confidence`. Em execucoes `low_confidence`, a UI deve informar que ainda nao ha dados suficientes para superar uma analise GPT ampla.

## Corpus minimo

Criar um arquivo ou conjunto de fixtures com `[50-100]` casos:

- `commander`
- `bracket`
- `decklist`
- `userIntent`
- `constraints`: budget, ownedOnly, avoidSalt, avoidTutors, preserveTheme, lowerCurve, improveMana, moreInteraction
- `expectedAdds`: lista curada por avaliacao humana
- `expectedCuts`: lista curada por avaliacao humana
- `protectedCards`: cartas que nao devem ser cortadas
- `notes`: contexto de mesa, arquétipo e motivo dos labels

## Baseline GPT

Para cada caso, gerar uma resposta GPT com prompt fixo:

```text
Improve this Magic: The Gathering Commander deck.
Respect Commander legality, color identity, budget, bracket, user preferences and the exact decklist.
Return concrete add/cut swaps with reasons.

[DECK_AND_USER_CONTEXT]
```

Registrar a saida GPT como artefato de benchmark, sem usa-la como fonte de verdade. O julgamento final deve comparar sistema e GPT contra labels humanos e invariantes.

## Metricas obrigatorias

- `commanderLegalityPassRate`: alvo 100%.
- `offColorAddRate`: alvo 0%.
- `duplicateAddRate`: alvo 0%.
- `commanderCutRate`: alvo 0%.
- `addPrecisionAt10`: proporcao de adds no top 10 aceitos pelo label humano.
- `cutPrecisionAt10`: proporcao de cuts no top 10 aceitos pelo label humano.
- `blindPreferenceWinRate`: avaliadores preferem o sistema ao GPT em teste cego; alvo inicial 60%+ nos casos cobertos.
- `actionabilityRate`: recomendacoes com add, cut, motivo, risco e impacto suficiente para aplicar ou rejeitar.
- `preferenceAdherenceRate`: aderencia a budget, ownedOnly, avoidSalt, avoidTutors e preserveTheme.

## Status por execucao

- `covered_by_internal_benchmark_reference`: comandante coberto por fixture/benchmark e execucao nao e baixa confianca.
- `benchmark_reference_exists_but_current_run_is_low_confidence`: existe referencia, mas falta cobertura/dados nesta execucao.
- `not_proven_against_gpt`: comandante ou contexto ainda nao coberto.

## Regra de produto

- Com `high_confidence`, o produto pode apresentar a recomendacao como fortemente fundamentada por dados, mantendo ressalvas de preco/disponibilidade/mesa.
- Com `medium_confidence`, o produto pode apresentar vantagem sobre GPT apenas se o comandante/bracket tiver benchmark coberto.
- Com `low_confidence`, o produto deve dizer explicitamente que nao ha dados suficientes para prometer qualidade superior ao GPT e continuar apenas com sugestoes conservadoras.
