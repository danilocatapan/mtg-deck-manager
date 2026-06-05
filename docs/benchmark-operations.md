# Operacao do Benchmark de Recomendacoes

Ultima atualizacao: 2026-06-05

## Estado Atual

- Corpus executavel: `backend/src/main/resources/recommendation-benchmark/cases-v1.json`.
- Tamanho atual: 20 casos em 8 comandantes.
- Meta para `benchmark_ready`: pelo menos 50 casos e 3 avaliacoes cegas por caso.
- Runner: `RecommendationBenchmarkService`.
- UI operacional: `frontend/src/pages/MetaAdminPage.jsx`.
- Persistencia: migration V14 e tabelas `recommendation_benchmark_*`.

O runner atual e offline e calcula metricas sobre saidas estruturadas e baselines GPT versionados. Ele nao chama Scryfall, TopDeck.gg ou OpenAI. Proxima evolucao tecnica: extrair o nucleo estrategico para executar diretamente cada fixture sem mudar os contratos administrativos.

## Fluxo Operacional

1. Administrador Google autorizado abre Meta Admin.
2. `Executar benchmark` chama `POST /meta/recommendation-benchmark/run`.
3. A rodada persiste versoes, resultados por caso e metricas.
4. A revisao A/B nunca revela qual opcao pertence ao sistema.
5. Cada caso exige 3 votos independentes; maioria simples define sistema, GPT ou empate.
6. Feedback de usuarios permanece separado e nao altera pesos automaticamente.

## Metricas

- `addPrecisionAt10` e `cutPrecisionAt10`: acertos sobre recomendacoes avaliadas.
- `preferenceAdherenceRate`: preferencias atendidas sobre preferencias declaradas.
- `actionabilityRate`: trocas com add, cut, justificativa e risco.
- `offColorDuplicateProtectedViolationRate`: violacoes de cor, duplicata ou corte protegido.
- `blindPreferenceWinRate`: casos completos em que a maioria preferiu o sistema.

## Logs e Diagnostico

Backend registra `benchmark.run.started`, `benchmark.run.completed`, `benchmark.run.failed`, `benchmark.review.recorded` e agregacoes em debug. Use apenas IDs, versoes, status, tempos e contagens.

O modo diagnostico frontend e desligado por padrao e dura somente a sessao. Quando ativado, escreve eventos sanitizados no painel e console. Nunca registrar token, identidade, decklist completa, notas privadas ou listas extensas de cartas.

## Teste Imediato

1. Ative Docker Desktop e rode a suite PostgreSQL descrita em `docs/postgresql-schema.md`.
2. Em `backend`, rode a suite completa com JDK 25.
3. Em `frontend`, rode lint, build, E2E e acessibilidade.
4. O Playwright deve validar executar benchmark, visualizar metricas, concluir quorum mockado, coletar feedback e acompanhar diagnostico sanitizado em desktop/mobile.
5. O fluxo de sucesso anexa screenshots dos marcos principais em `frontend/test-results`; os artefatos complementam os asserts e ajudam na auditoria visual.

## Proximos Passos

1. Expandir de 20 para 50 casos.
2. Extrair o nucleo estrategico para execucao direta das fixtures.
3. Completar 3 votos humanos por caso.
4. Investigar metricas abaixo da meta.
5. Propor calibracao somente com evidencia e sem regressao Commander.
