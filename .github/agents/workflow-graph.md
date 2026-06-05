# workflow-graph.md - Guia do hotspot: pipeline de recomendacao

Versao: agents-2026-06-05
Ultima atualizacao: 2026-06-05

Quando usar
-----------
- Ao alterar ou refatorar o fluxo de recomendacao (candidatos -> filtro -> scoring -> completar -> cortes).

Invariantes do grafo
--------------------
- Ordem das etapas e importante: coleta de candidatos -> filtragem por cor/duplicidade -> scoring (meta + synergy + top decks quando elegiveis) -> rank -> completar ate 99 -> sugestoes de corte -> auditoria/aplicacao quando solicitada.
- Cada etapa deve ser idempotente e testavel isoladamente.
- O pipeline nao deve sugerir cartas duplicadas nem cartas fora da color identity.
- Sugestoes de corte nao devem remover comandante.
- Meta top decks devem respeitar amostra minima, formato, bracket e sourceMode antes de influenciar ranking.
- Apply/undo swap nao pode alterar carta errada nem quebrar quantidade total; deve preservar rastreabilidade da recomendacao aplicada.
- O benchmark offline executa diretamente `StrategicRecommendationEngine` por `RecommendationBenchmarkScenarioService`; nao pode acessar rede, banco externo ou auditoria.
- A geracao GPT exige 50 snapshots reais completos, um comandante por caso, procedencia HTTPS e fonte aprovada. Corpus incompleto deve retornar `corpus_not_ready`.
- Julgamentos GPT recebem somente A/B. O backend mapeia a identidade depois e aplica vetos objetivos antes da chamada.
- Claims no runtime dependem de `benchmarkEvidence` dinamico do conjunto promovido por comandante/bracket; nunca adicionar listas fixas de comandantes cobertos.
- A UI comum mostra apenas resumo confiavel da evidencia. Diagnosticos e comparacoes completas ficam no Meta Admin.

Pontos do codigo que merecem revisao explicita
----------------------------------------------
- `RecommendationService` - orquestracao das recomendacoes heuristicas.
- `StrategicRecommendationService` - recomendacoes estrategicas e criterios de decisao.
- `StrategicRecommendationEngine`, `RecommendationBenchmarkScenarioService` e `RecommendationBenchmarkAiService` - decisao offline, validacao do corpus e comparacao automatica.
- `MetaProvider`, `MetaProviderImpl`, `MetaDatasetLoader`, `MetaDatasetService` - validacao de dados e normalizacao de nomes.
- `TopDeckMetaAdapter`, `ExternalMetaIngestionJob`, `MetaDatasetService`, `BracketMetaPolicy` - sync automatico, persistencia canonica e politica de uso no ranking.
- `ColorIdentityMatcher`, `CandidateAddSelector`, `CandidateCutSelector` - regras de cor, duplicidade e elegibilidade.
- `DeckCompleter` e `RecommendationPairer` - completar deck e parear adds/cuts.
- `SynergyEngine`, `CardTagger` e `RecommendationScoring` - tags, pesos e combinacoes (meta vs synergy).
- `RecommendationAuditService` - auditoria de recomendacoes geradas, feedback e trocas aplicadas/desfeitas.

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
