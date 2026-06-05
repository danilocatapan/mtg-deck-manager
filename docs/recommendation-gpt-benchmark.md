# Benchmark: recomendador Commander vs GPT

Ultima atualizacao: 2026-06-05

## Objetivo

Medir se o recomendador oferece vantagem automatica qualificada sobre GPT-5.5 sem confundir popularidade, julgamento automatico ou feedback com validacao humana.

## Corpus Qualificavel

O arquivo `backend/src/main/resources/recommendation-benchmark/cases-v1.json` ainda possui 20 cenarios reduzidos e serve somente para diagnostico do runner.

A geracao e promocao GPT exigem exatamente 50 snapshots reais:

- um comandante diferente por caso;
- comandante mais 99 cartas;
- `source`, `sourceUrl` HTTPS e `capturedAt`;
- somente fontes `archidekt_popular` ou `topdeck_tournament`;
- preferencias, restricoes, catalogo e meta congelados;
- labels independentes apenas quando realmente existentes.

Selecao:

- casual/mid/popular: decks Commander publicos e completos do Archidekt, ordenados por visualizacoes e deduplicados por comandante;
- high-power/cEDH: decks com resultado real de torneio via TopDeck.gg;
- popularidade nunca deve ser tratada como evidencia de competitividade.

`RecommendationBenchmarkScenarioService` valida os snapshots. A geracao GPT retorna `corpus_not_ready` enquanto o corpus nao estiver completo e auditavel.

Coletores: `tools/collect-archidekt-benchmark-candidates.ps1` congela manifesto/lista/catalogo; `tools/collect-topdeck-benchmark-candidates.ps1` seleciona listas competitivas distintas e exige `TOPDECK_API_KEY`. O catalogo TopDeck precisa ser enriquecido antes da qualificacao.

## Execucao Offline

`RecommendationBenchmarkService` executa diretamente `StrategicRecommendationEngine`, sem rede, banco externo ou auditoria de usuario. O servico estrategico de runtime prepara dados persistidos e delega ao mesmo engine.

## Baselines e Juiz GPT

O fluxo administrativo usa Responses API, `store=false`, modelo fixo `gpt-5.5`, output estruturado e concorrencia maxima 2.

Cada caso recebe:

- baseline generico com deck e preferencias;
- baseline grounded com o mesmo catalogo e meta congelados;
- tres julgamentos cegos por baseline.

O juiz recebe somente opcoes A/B. O backend mapeia a identidade depois da resposta. Violacoes objetivas de Commander causam veto deterministico antes do juiz. Artefatos compativeis sao retomados por hashes que incluem as respostas julgadas. Conjuntos incompletos nunca substituem o ultimo conjunto promovido.

O conjunto promovido cria evidencia por comandante/bracket. O runtime retorna `benchmarkEvidence` como `qualified_advantage`, `covered_not_qualified` ou `not_covered`; nunca usa lista fixa de comandantes.

## Readiness

`automatic_benchmark_ready` exige:

- 50 casos reais completos executados pelo engine;
- nenhuma violacao Commander;
- `actionabilityRate >= 90%`;
- `preferenceAdherenceRate >= 80%`;
- sistema vencedor em pelo menos 60% dos casos contra ambos os baselines;
- empates de no maximo 20%;
- artefatos GPT completos e atuais.

`addPrecisionAt10` e `cutPrecisionAt10` permanecem `not_ready` sem labels independentes suficientes. Resultados nunca alteram pesos automaticamente.

## Validacao Humana

A revisao humana cega permanece disponivel e planejada, mas nao bloqueia a primeira fase automatica. Toda comunicacao deve dizer "vantagem automatica qualificada" e explicitar que a validacao humana ainda esta pendente.
