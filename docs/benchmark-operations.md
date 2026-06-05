# Operacao do Benchmark de Recomendacoes

Ultima atualizacao: 2026-06-05

## Estado Atual

- Corpus diagnostico: 20 cenarios reduzidos em `recommendation-benchmark/cases-v1.json`.
- Meta qualificavel: 50 decks reais completos e comandantes distintos.
- Runner offline: `RecommendationBenchmarkService`.
- Engine: `StrategicRecommendationEngine`.
- Conversao e validacao: `RecommendationBenchmarkScenarioService`.
- Artefatos GPT: `RecommendationBenchmarkAiService`.
- UI: `frontend/src/pages/MetaAdminPage.jsx`.
- Persistencia: migrations V14/V15.

O runner offline continua utilizavel durante a coleta. A geracao GPT bloqueia com `corpus_not_ready` ate os 50 snapshots auditaveis estarem congelados.

## Fluxo Operacional

1. Execute o benchmark offline no Meta Admin.
2. Consulte a previsao GPT para conferir modelo, configuracao e quantidade maxima de chamadas.
3. Gere comparacoes somente quando o corpus estiver qualificavel e `OPENAI_API_KEY` estiver configurada.
4. A geracao cria baseline generico, baseline grounded e tres julgamentos A/B cegos por baseline.
5. Vetos objetivos de Commander ocorrem antes do juiz GPT.
6. Somente um conjunto completo pode ser promovido; falhas preservam o anterior.
7. Revisao humana e feedback permanecem separados e nunca ajustam pesos automaticamente.

## Fontes do Corpus

- Archidekt: ranking publico por visualizacoes para popularidade/casual/mid.
- TopDeck.gg: resultados e decklists de torneios para high-power/cEDH; exige `TOPDECK_API_KEY` e atribuicao.
- Cada snapshot registra fonte, URL e data de captura.
- Pular listas incompletas, comandantes repetidos e listas sem procedencia.

Para atualizar o manifesto popular sem alterar automaticamente o corpus:

```powershell
./tools/collect-archidekt-benchmark-candidates.ps1 -Target 25
```

O resultado fica em `backend/src/main/resources/recommendation-benchmark/archidekt-candidates.json`. Revise e congele os snapshots completos antes de incorporá-los a `cases-v1.json`.

## APIs

- `POST /meta/recommendation-benchmark/run`
- `GET /meta/recommendation-benchmark/summary`
- `POST /meta/recommendation-benchmark/ai-artifacts/preview`
- `POST /meta/recommendation-benchmark/ai-artifacts/generate`
- `GET /meta/recommendation-benchmark/ai-artifacts/jobs/{id}`
- `GET /meta/recommendation-benchmark/cases/{caseId}/comparison`

## Logs e Diagnostico

Backend registra somente IDs, versoes, hashes, status, tempos e contagens. O diagnostico frontend e opt-in e dura somente a sessao. Nunca registrar chaves, identidade, prompts completos ou decklists completas.

## Validacao

- Backend H2 completo e migration PostgreSQL V1-V15.
- Smoke OpenAI real somente opt-in.
- Frontend lint/build.
- Playwright desktop/mobile para preview, job, progresso, falha preservada, comparacao, claim qualificado e screenshots.
- `git diff --check`.

## Pendencias

1. Congelar 50 decks reais completos e distintos.
2. Configurar `TOPDECK_API_KEY` para a parcela competitiva.
3. Executar smoke OpenAI e promover o primeiro conjunto.
4. Completar validacao humana posterior.
