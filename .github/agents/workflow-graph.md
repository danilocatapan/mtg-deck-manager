# workflow-graph.md - Guia do hotspot: pipeline de recomendacao

Quando usar
-----------
- Ao alterar ou refatorar o fluxo de recomendacao (candidatos -> filtro -> scoring -> completar -> cortes).

Invariantes do grafo
--------------------
- Ordem das etapas e importante: coleta de candidatos -> filtragem por cor/duplicidade -> scoring (meta + synergy) -> rank -> completar ate 99 -> sugestoes de corte.
- Cada etapa deve ser idempotente e testavel isoladamente.
- O pipeline nao deve sugerir cartas duplicadas nem cartas fora da color identity.
- Sugestoes de corte nao devem remover comandante.

Pontos do codigo que merecem revisao explicita
----------------------------------------------
- `RecommendationService` - orquestracao das recomendacoes heuristicas.
- `StrategicRecommendationService` - recomendacoes estrategicas e criterios de decisao.
- `MetaProvider`, `MetaProviderImpl`, `MetaDatasetLoader` - validacao de dados e normalizacao de nomes.
- `ColorIdentityMatcher`, `CandidateAddSelector`, `CandidateCutSelector` - regras de cor, duplicidade e elegibilidade.
- `DeckCompleter` e `RecommendationPairer` - completar deck e parear adds/cuts.
- `SynergyEngine`, `CardTagger` e `RecommendationScoring` - tags, pesos e combinacoes (meta vs synergy).

Validacoes minimas
------------------
Depois de qualquer alteracao no grafo:
- Rodar testes unitarios dos componentes afetados.
- Rodar testes de controller/service quando contrato ou orquestracao mudar.
- Executar um fluxo representativo com deck de exemplo e validar:
  - nenhuma carta fora de cor;
  - total coerente com o alvo Commander;
  - sem duplicatas;
  - ranking coerente com meta quando presente;
  - cuts coerentes com a estrategia e sem remover comandante.

Saida esperada
--------------
- PR ou resumo com alteracao de codigo, testes executados, um caso de entrada/saida quando aplicavel e validacao dos invariantes acima.
