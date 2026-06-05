# Operacao do Benchmark de Recomendacoes

Ultima atualizacao: 2026-06-05

## Estado Atual

- Corpus diagnostico: 20 cenarios reduzidos em `recommendation-benchmark/cases-v1.json`.
- Corpus real popular: 25 snapshots Archidekt completos, distintos e validados em `archidekt-snapshots.json`.
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
./tools/collect-topdeck-benchmark-candidates.ps1 -Target 25
```

Os resultados ficam em `archidekt-candidates.json`, `archidekt-snapshots.json` e `topdeck-snapshots.json`. O coletor TopDeck exige `TOPDECK_API_KEY`; seu catalogo vazio impede qualificacao ate o enriquecimento congelado. Revise os 50 snapshots antes de incorporá-los ao corpus ativo.

O Meta Admin exibe o funil candidatos -> snapshots -> casos validos -> runner offline -> artefatos GPT -> conjunto promovido. Bloqueadores aparecem como acoes legiveis e nunca exigem importacao manual de JSON.

## Credenciais e Responsabilidades

- `TOPDECK_API_KEY`: crie gratuitamente em `https://topdeck.gg/account` e configure somente no `.env` local como `TOPDECK_API_KEY=<chave>`. Ela autoriza o backend/coletor a consultar torneios, classificacoes e decklists pela API TopDeck.gg; nunca deve ser enviada em chat ou commitada.
- `OPENAI_API_KEY`: deve permanecer somente no backend. O smoke real de 2026-06-05 alcançou a Responses API, mas retornou `insufficient_quota`; a conta precisa de saldo ou limite disponível antes de gerar e promover artefatos.
- Acao do responsavel pela conta: criar a chave TopDeck e regularizar o saldo/limite da OpenAI API.
- Acao executavel pelo Codex apos isso: coletar e enriquecer os 25 casos TopDeck, validar os 50 casos, repetir smoke/benchmark e promover o primeiro conjunto completo.

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

- Backend H2 completo: 198 testes passaram em 2026-06-05.
- PostgreSQL 16 real e isolado: 198 testes passaram e Flyway aplicou V1-V15 sobre schema vazio em 2026-06-05.
- Smoke OpenAI real somente opt-in.
- Frontend lint/build.
- Playwright desktop/mobile para preview, job, progresso, falha preservada, comparacao, claim qualificado e screenshots.
- `git diff --check`.

## Pendencias

1. Configurar `TOPDECK_API_KEY` e congelar os 25 decks competitivos restantes.
2. Formar o corpus ativo de 50 casos, adicionar saldo/limite à OpenAI API e repetir o smoke.
3. Promover o primeiro conjunto completo.
4. Completar validacao humana posterior.
