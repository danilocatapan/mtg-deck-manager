# workflow-graph.md — Guia do hotspot: pipeline de recomendação (grafo de etapas)

Quando usar
-----------
- Ao alterar ou refatorar o fluxo de recomendação (candidatos → filtro → scoring → completar → cortes).

Invariantes do grafo
---------------------
- Ordem das etapas é importante: coleta de candidatos → filtragem por cor/duplicidade → scoring (meta+synergy) → rank → completar até 99 → sugestões de corte.
- Cada etapa deve ser idempotente e testável isoladamente.

Pontos do código que merecem revisão explícita
----------------------------------------------
- `RecommendationService` (orquestra) — revisar se a orquestração foi alterada.  
- `MetaProvider`/`MetaDatasetLoader` — validação de dados; normalização de nomes.  
- `CardFilter` (se existir) — regras de color identity e legality.  
- `SynergyEngine` e `RecommendationScoring` — pesos e combinações (meta vs synergy).

Validações mínimas
------------------
- Depois de qualquer alteração no grafo:  
  - rodar testes unitários de cada componente;  
  - executar um fluxo end-to-end com um deck de exemplo e validar:  
    - nenhuma carta fora de cor;  
    - total de 99 cartas adicionadas;  
    - sem duplicatas;  
    - ranking coerente com meta quando presente.

Saída esperada
--------------
- PR com: alteração de código, lista mínima de testes executados, um caso de entrada/saída (ex.: deck X → top 10 adds), e validação de invariantes acima.
