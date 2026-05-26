# MTG Deck Manager - Contexto do Projeto

Versao: context-2026-05-26
Ultima atualizacao: 2026-05-26

## Objetivo

MTG Deck Manager e uma aplicacao para gerenciar, importar, consultar, analisar e melhorar decks de Magic: The Gathering, com foco em Commander. O produto combina CRUD de decks, vitrine publica, privacidade/LGPD, analise deterministica e recomendacoes explicaveis de adds/cuts baseadas em regras, sinergia, meta local e top decks importados.

## Apps

- `backend/`: API REST em Java/Quarkus.
- `frontend/`: SPA em React/Vite publicada com base path `/mtg-deck-manager/`.
- `tools/meta-importer/`: scripts offline para gerar/importar dados de meta; scraping/coleta externa nao deve acontecer em runtime de API quando puder ser feito offline.

## Stack Atual

- Backend: Java 25, Quarkus 3.35.2, RESTEasy Reactive/Quarkus REST, Jackson, SmallRye OpenAPI.
- Persistencia: Hibernate ORM + Panache; H2 em `%dev`/`%test`; PostgreSQL 16+ em `%pg`/`%prod`; Flyway em `backend/src/main/resources/db/migration`.
- Auth: Google OIDC via ID token Bearer. A API nao usa cookie de sessao.
- Integracoes: Scryfall, Spicerack, TopDeck.gg, Commander Spellbook para cache local de combos, dataset local de meta e regras Commander em resources.
- Frontend: React 19.2, Vite 8, ESLint 10, estado local React, sem router dedicado.
- CI/CD: GitHub Actions com backend H2, backend PostgreSQL, lint/build frontend, versionamento por tag, imagem backend GHCR, deploy backend por hook, smoke manual e GitHub Pages.

## Arquitetura Backend

Camadas principais:

- `controller`: endpoints REST e contratos HTTP.
- `dto`: payloads publicos e administrativos.
- `model`/`repository`: entidades Panache/Hibernate e consultas.
- `service`: regras de negocio, decks, analise, recomendacoes, auditoria, privacidade e integracoes.
- `service/meta`: dataset local, top decks, adapters Spicerack/TopDeck/EDHREC, normalizacao e agregacao.
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
- Ingestao de meta local, decks externos, top decks rankeados e combos conhecidos cacheados para grounding deterministico.
- Admin de meta top decks no frontend para usuario Google autorizado.
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
- `POST /meta/top-decks/import`
- `GET /meta/top-decks`
- `GET /meta/top-decks/{id}`
- `POST /meta/top-decks/sync`
- `POST /meta/combos/sync`

### Auditoria, LGPD e Seguranca

- `POST /recommendation-audits/{id}/feedback`
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

- Migrations Flyway vivem em `backend/src/main/resources/db/migration` e hoje cobrem schema de decks, historico, auditoria, visibilidade, decks externos, likes, meta top decks e cache local de combos.
- Dados locais de meta:
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
- Atualizar este arquivo quando mudarem objetivo, stack, endpoints, arquitetura, CI/CD, persistencia, seguranca, privacidade ou fluxos principais.
- Atualizar `CHANGELOG.md` para mudancas relevantes. Release notes publicas so recebem itens que afetam usuarios/suporte.
