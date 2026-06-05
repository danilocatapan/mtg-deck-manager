# MTG Deck Manager - Contexto do Projeto

Versao: context-2026-06-05
Ultima atualizacao: 2026-06-05

## Leitura em 60 segundos

- Objetivo: gerenciar e melhorar decks Commander com recomendacoes explicaveis, legais, personalizadas e verificaveis; afirmar vantagem sobre GPT apenas quando benchmark e cobertura sustentarem isso.
- Stack: Java 25 + Quarkus 3.35.2 em `backend/`, React 19 + Vite 8 em `frontend/`, Hibernate/Panache com H2 para testes e PostgreSQL/Flyway para producao.
- Fluxo de recomendacao: deck + preferencias -> meta elegivel -> candidatos -> filtros Commander -> score/sinergia -> adds/cuts -> quality gate -> auditoria/feedback.
- Invariantes: respeitar color identity, nao sugerir duplicata, nao cortar comandante, manter quantidade coerente e usar top decks somente com fonte/bracket/amostra validos.
- Validacao essencial: backend com JDK 25 e `./mvnw.cmd test`; frontend com `npm run lint` e `npm run build`; mudancas de recomendacao tambem exigem caso representativo.
- Entrada por tarefa: recomendacao -> `StrategicRecommendationService` + `.github/agents/workflow-graph.md`; backend/contrato -> controller/service/test + `.github/agents/backend-quarkus.md`; frontend -> page/component/API + `.github/agents/frontend-react.md`; testes -> `.github/agents/testing.md`.
- Regra para agentes: use primeiro este snapshot; abra codigo adicional com `rg` apenas para confirmar o trecho diretamente afetado ou quando este documento marcar a capacidade como parcial/planejada.

## Estado Atual Verificado

| Capacidade | Estado | Evidencia e limite atual |
| --- | --- | --- |
| Quality gate, confianca e limitacoes | `pronto` | `StrategicRecommendationRun` e `StrategicRecommendationService` expoem confidence, coverage, freshness, fontes, limitacoes e `benchmarkStatus`; UI apresenta aviso em baixa confianca. |
| Personalizacao por estrategia, orcamento e colecao | `pronto` | `strategy` altera scoring, `budget` filtra/penaliza candidatos e `ownedOnly` usa a colecao persistida; sem inventario, o quality gate cai para baixa confianca. |
| TopDeck.gg e Meta Admin | `pronto` | TopDeck.gg e a unica fonte externa viva; `/meta/sync` busca, persiste na base canonica, reconstrói perfis e separa importados, descartados, snapshot preservado e erros operacionais. |
| Benchmark contra GPT | `parcial` | Runner offline calcula metricas sobre 20 casos versionados e persiste rodadas/resultados; ainda faltam 50 casos e execucao direta do nucleo estrategico para prova mais forte. |
| Baseline GPT e avaliacao humana | `parcial` | Baselines estruturados entram nos 20 casos e o Meta Admin oferece revisao A/B cega; ainda faltam 3 votos por caso para fechar o quorum. |
| Cobertura meta por comandante | `parcial` | Perfis locais dedicados cobrem Xenagos, K'rrik, Grand Arbiter e Kess; ainda nao ha cobertura ampla de comandantes populares e long-tail. |
| Feedback agregado e calibracao automatica | `parcial` | Meta Admin agrega feedback por comandante, bracket, status e motivo; pesos continuam sem ajuste automatico por regra. |
| Pet cards e cartas intocaveis | `planejado` | Pecas de combo e valor estrategico recebem protecao, mas o usuario ainda nao pode marcar explicitamente cartas intocaveis. |

Estado da prova: a fundacao verificavel esta pronta, mas o produto ainda nao demonstrou superioridade global sobre GPT. Claims devem permanecer restritos a execucoes com cobertura e benchmark suficientes.

## Proximas Prioridades

1. Expandir o corpus calculado de 20 para pelo menos 50 casos completos.
2. Extrair o nucleo estrategico para o runner executar diretamente o algoritmo com fixtures offline, substituindo snapshots versionados de saida.
3. Completar 3 avaliacoes cegas por caso e analisar `systemWins`, `gptWins` e `tie`.
4. Investigar metricas abaixo da meta usando auditoria e diagnostico sanitizado.
5. Calibrar scoring somente quando o benchmark demonstrar melhora sem regressao dos invariantes Commander.

## Mapa Rapido para Agentes

- Recomendacao: comece em `backend/src/main/java/com/mtg/service/StrategicRecommendationService.java`, depois selectors/pairer e `backend/src/test/java/com/mtg/service/StrategicRecommendationServiceTest.java`; preserve o grafo descrito em `.github/agents/workflow-graph.md`.
- Benchmark: consulte `docs/recommendation-gpt-benchmark.md`, `docs/benchmark-operations.md`, `backend/src/main/resources/recommendation-benchmark/cases-v1.json` e `RecommendationBenchmarkService`.
- Meta/TopDeck: comece em `TopDeckMetaAdapter`, `ExternalMetaIngestionJob`, `MetaDatasetService` e `MetaDeckSnapshot`; ingestao externa deve respeitar API, attribution, rate limit e preservacao do ultimo snapshot.
- Colecao/privacidade: comece em `UserCollectionService`, `UserPrivacyController`, migration `V12__create_user_card_collection.sql` e `UserPrivacyControllerTest`; preserve isolamento por usuario e exportacao/exclusao LGPD.
- Frontend: recomendacoes em `frontend/src/components/recommendations/`, Meta Admin em `frontend/src/pages/MetaAdminPage.jsx` e contratos HTTP em `frontend/src/services/api.js`.
- Validacao conhecida em 2026-06-05: backend `./mvnw.cmd test` com JDK 25 passou com 195 testes e zero falhas; frontend lint, build, 12 cenarios E2E e 6 cenarios de acessibilidade passaram. O fluxo Meta Admin gerou 14 screenshots Playwright auditaveis em desktop/mobile. PostgreSQL local aguarda Docker daemon ativo.

## Objetivo

MTG Deck Manager e uma aplicacao para gerenciar, importar, consultar, analisar e melhorar decks de Magic: The Gathering, com foco em Commander. O produto combina CRUD de decks, vitrine publica, privacidade/LGPD, analise deterministica e recomendacoes explicaveis baseadas em regras, sinergia, meta local e snapshot automatico TopDeck.gg.

## Apps

- `backend/`: API REST em Java/Quarkus.
- `frontend/`: SPA em React/Vite publicada com base path `/mtg-deck-manager/`.
- `tools/`: scripts operacionais; meta competitivo entra somente pela API documentada TopDeck.gg e o fallback local permanece versionado em resources.

## Stack Atual

- Backend: Java 25, Quarkus 3.35.2, RESTEasy Reactive/Quarkus REST, Jackson, SmallRye OpenAPI.
- Persistencia: Hibernate ORM + Panache; H2 em `%dev`/`%test`; PostgreSQL 16+ em `%pg`/`%prod`; Flyway em `backend/src/main/resources/db/migration`.
- Auth: Google OIDC via ID token Bearer. A API nao usa cookie de sessao.
- Integracoes: Scryfall, TopDeck.gg como unica fonte externa viva de meta, Commander Spellbook para cache local de combos, dataset local de fallback e regras Commander em resources.
- Frontend: React 19.2, Vite 8, ESLint 10, estado local React, sem router dedicado.
- CI/CD: GitHub Actions com backend H2, cenário PostgreSQL legado V1-V12 -> V13/V14, suíte backend PostgreSQL, lint/build/Playwright frontend, versionamento por tag, imagem backend GHCR, deploy backend por hook, smoke manual e GitHub Pages.

## Arquitetura Backend

Camadas principais:

- `controller`: endpoints REST e contratos HTTP.
- `dto`: payloads publicos e administrativos.
- `model`/`repository`: entidades Panache/Hibernate e consultas.
- `service`: regras de negocio, decks, analise, recomendacoes, auditoria, privacidade e integracoes.
- `service/meta`: dataset local de fallback, adapter TopDeck.gg, persistencia canonica, normalizacao e agregacao.
- `service/synergy`: tags e motor de sinergia.
- `service/rules`: banlist, brackets e game changers Commander.
- `client`: REST clients externos.
- `config`: headers de seguranca, logs estruturados/sanitizados, mappers e contexto de request.

Controllers devem ficar finos. Regra de negocio deve viver em services/componentes testaveis.

## Arquitetura Frontend

- `src/pages`: telas principais (`Home`, editor, importacao, consulta publica, meta admin, contato, release notes).
- `src/components`: componentes de UI e composicao.
- `src/components/recommendations`: painel/cards/configuracoes de recomendacao.
- `src/components/ui`: primitivos simples.
- `src/services/api.js`: cliente REST central, timeout, retry de startup, `Authorization: Bearer`, `credentials: omit`.
- `src/services/auth.js`: sessao Google em `sessionStorage`, validacao de issuer/audience/exp e admin meta por e-mail allowlistado.
- `src/assets` e `public`: identidade visual, release notes publicas, politica de privacidade e icones.

## Funcionalidades Principais

- Busca de cartas por nome e collection API da Scryfall.
- CRUD autenticado de decks privados/publicos.
- Importacao e exportacao de decklists.
- Vitrine publica com consulta read-only, filtro por comandante, copia de deck, likes e ranking.
- Analise de deck: curva auditavel por cartas, CMC, roles, combos, probabilidade, mana base, comparacao e legalidade Commander.
- Recomendacoes heuristicas e estrategicas com justificativas, score detalhado, adds/cuts, apply/undo swap e feedback/auditoria.
- Ingestao automatica TopDeck.gg em snapshot canonico, fallback local embarcado, decks externos da vitrine e combos conhecidos cacheados para grounding deterministico.
- Meta Admin operacional para usuario Google autorizado, sem importacao manual de decklists.
- Exportacao e exclusao de dados do usuario autenticado.
- Diagnostico administrativo read-only de postura de seguranca.
- Paginas publicas de contato, privacidade, release notes e Sobre.

## Endpoints Atuais

### Info

- `GET /`: metadados basicos da API.
- `GET /app/info`: metadados de build/runtime para UI.

### Cards

- `GET /cards?name=`
- `POST /cards/collection`

### Decks Autenticados/Compatibilidade

- `GET /decks`
- `POST /decks`
- `POST /decks/import`
- `GET /decks/public` (compatibilidade; vitrine nova usa `/public/decks`)
- `GET /decks/{id}`
- `GET /decks/{id}/consult`
- `PUT /decks/{id}`
- `DELETE /decks/{id}`
- `GET /decks/{id}/export`
- `GET /decks/{id}/analysis`
- `GET /decks/{id}/legality`
- `POST /decks/{id}/comparison`
- `POST /decks/{id}/recommendations`
- `POST /decks/{id}/recommendations/strategic`
- `POST /decks/{id}/recommendations/apply-swap`
- `POST /decks/{id}/recommendations/undo-swap`

### Publico

- `GET /public/decks`
- `GET /public/decks/top`
- `GET /public/decks/{id}`
- `POST /public/decks/{id}/copy`
- `POST /public/decks/{id}/like`
- `DELETE /public/decks/{id}/like`

### Meta

- `GET /meta/sources`
- `POST /meta/sync`
- `GET /meta/decks`
- `POST /meta/rebuild-profiles`
- `GET /meta/commanders/{commander}`
- `POST /meta/external-decks/import`
- `POST /meta/combos/sync`
- `GET /meta/recommendation-benchmark/summary`
- `POST /meta/recommendation-benchmark/run`
- `GET /meta/recommendation-benchmark/reviews/next`
- `POST /meta/recommendation-benchmark/reviews/{caseId}`

### Auditoria, LGPD e Seguranca

- `POST /recommendation-audits/{id}/feedback`
- `GET /users/me/collection`
- `POST /users/me/collection/import`
- `GET /users/me/export`
- `DELETE /users/me`
- `POST /security/status/check`

## Fluxo de Recomendacao

1. Carrega deck, comandante e cartas persistidas.
2. Normaliza nomes, roles, color identity e contexto Commander.
3. Busca candidatos em meta local, top decks elegiveis, heuristicas, sinergia e cartas conhecidas.
4. Filtra cartas fora da color identity, duplicatas e candidatos inelegiveis.
5. Analisa curva, papeis, combos, mana base, bracket e necessidades do deck.
6. Pontua candidatos por papel, sinergia, meta, bracket, estrategia e impacto esperado.
7. Ordena adds e pareia cortes coerentes.
8. Completa ate o alvo Commander quando aplicavel.
9. Retorna recomendacoes explicaveis e auditaveis.
10. Quando o usuario aplica/desfaz troca, registra auditoria sem vazar tokens/PII em logs.

Invariantes:

- Deck Commander deve respeitar color identity.
- Nao sugerir carta ja existente como add.
- Nao cortar comandante.
- Fluxos de completar deck miram 99 cartas no main deck + comandante = 100.
- Top decks so influenciam ranking quando fonte/formato/bracket/amostra minima forem validos.
- Deck privado nao pode aparecer em endpoints publicos nem ser consultado por anonimo/outro usuario.

## Persistencia e Dados

- Migrations Flyway vivem em `backend/src/main/resources/db/migration` e hoje cobrem schema de decks, historico, auditoria, visibilidade, decks externos, likes, snapshot canonico de meta e cache local de combos.
- Dados de meta:
  - tabelas `meta_decks` e `meta_deck_cards` para snapshots TopDeck.gg normalizados;
  - `backend/src/main/resources/meta_dataset.json`
  - `backend/src/main/resources/meta/commanders/`
- Regras Commander:
  - `backend/src/main/resources/rules/commander-banlist.json`
  - `backend/src/main/resources/rules/commander-game-changers.json`
- Combos conhecidos:
  - `backend/src/main/resources/analysis/known-combos.json`
  - tabelas `meta_combos` e `meta_combo_cards`, sincronizadas por `POST /meta/combos/sync` e usadas antes do JSON embarcado quando houver dados.

## Privacidade e Seguranca

- Escopos Google: `openid`, `email`, `profile`.
- Frontend armazena ID token e perfil publico apenas em `sessionStorage`.
- Backend valida ID token como Bearer token e nao persiste `access_token` nem `refresh_token`.
- Decks sem `visibility` devem assumir `private`.
- DTOs publicos nao devem expor `owner_id`, e-mail, avatar, historico de trocas ou auditorias.
- Logs nao devem conter tokens, cookies, Authorization, payloads completos de decks, nomes de cartas em massa ou PII desnecessaria.
- Diagnostico frontend e opt-in por sessao e registra somente IDs, status, contagens, confidence, bracket e numero de limitacoes/fontes.
- Endpoints admin de meta aceitam Google JWT allowlistado por e-mail e mantem `X-Admin-Key` para operacao tecnica/Swagger.

## Validacao

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm run lint
npm run build
```

PostgreSQL local:

```powershell
docker compose up -d postgres
cd backend
$env:QUARKUS_DATASOURCE_DB_KIND = "postgresql"
$env:QUARKUS_DATASOURCE_JDBC_URL = "jdbc:postgresql://localhost:5432/mtg_deck_manager"
$env:QUARKUS_DATASOURCE_USERNAME = "mtg"
$env:QUARKUS_DATASOURCE_PASSWORD = "mtg_dev_password"
$env:QUARKUS_FLYWAY_MIGRATE_AT_START = "true"
$env:QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY = "validate"
.\mvnw.cmd test
```

Quando mudar recomendacoes, importacao, persistencia, privacidade ou UI principal, validar tambem um fluxo representativo.

## Manutencao Documental

- `AGENTS.md` da raiz e `.github/copilot-instructions.md` sao pontos de entrada; a fonte canonica e `.github/agents/AGENTS.md`.
- O Meta Admin deriva proximas acoes do benchmark e nunca solicita importacao manual de top decks.
- Atualizar este arquivo quando mudarem objetivo, stack, endpoints, arquitetura, CI/CD, persistencia, seguranca, privacidade ou fluxos principais.
- Atualizar `CHANGELOG.md` para mudancas relevantes. Release notes publicas so recebem itens que afetam usuarios/suporte.
