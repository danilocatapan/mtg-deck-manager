# MTG Deck Manager

Versao docs: 2026-05-21
Ultima atualizacao: 2026-05-21

Aplicacao para cadastrar, importar, consultar, analisar e otimizar decks Commander. O projeto tem backend Quarkus, frontend React/Vite, persistencia PostgreSQL em producao e recomendacoes explicaveis baseadas em regras, sinergia e dados de meta.

## Modulos

- `backend/`: API REST Java 25 + Quarkus 3.35.2.
- `frontend/`: SPA React 19 + Vite 8, publicada em `/mtg-deck-manager/`.
- `tools/`: scripts operacionais de seguranca e migracao.
- `.github/agents/`: instrucoes canonicas para Codex/Copilot.

## Funcionalidades

- Login Google por ID token Bearer.
- CRUD de decks privados/publicos.
- Vitrine publica com consulta, copia, likes e ranking.
- Importacao/exportacao de decklists.
- Analise de curva, papeis, combos, legalidade e comparacao.
- Recomendacoes estrategicas com adds/cuts, apply/undo swap, feedback e auditoria.
- Ingestao/admin de top decks para enriquecer meta.
- Exportacao/exclusao de dados do usuario.

## Execucao Local

Backend em dev com H2:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd backend
.\mvnw.cmd quarkus:dev
```

Backend com PostgreSQL local:

```powershell
docker compose up -d postgres
cd backend
.\mvnw.cmd quarkus:dev -Dquarkus.profile=pg
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

URLs comuns:

- Frontend: `http://localhost:5173/mtg-deck-manager/`
- Backend: `http://localhost:8080`
- OpenAPI: `http://localhost:8080/swagger`
- Swagger UI quando habilitado: `http://localhost:8080/swagger-ui`
- Quarkus Dev UI: `http://localhost:8080/q/dev/`

## Validacao

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd frontend
npm run lint
npm run build
```

Mudancas em recomendacoes devem cobrir color identity, duplicatas, tamanho do deck, cortes e coerencia de bracket/meta. Mudancas em persistencia devem avaliar migration Flyway e teste PostgreSQL.

## Privacidade e Seguranca

- Escopos Google: `openid`, `email`, `profile`.
- Backend valida ID token recebido via `Authorization: Bearer <google-id-token>`.
- Frontend guarda token e perfil somente em `sessionStorage`; chamadas usam `credentials: omit`.
- Decks sem `visibility` assumem `private`.
- Respostas publicas usam DTOs sanitizados e nao expoem `owner_id`, e-mail, avatar, auditorias ou historico de trocas.
- `GET /users/me/export` exporta dados do usuario autenticado.
- `DELETE /users/me` remove decks, likes e auditorias vinculados ao usuario autenticado.
- Logs nao devem registrar tokens, cookies, header `Authorization`, payload completo de deck ou PII desnecessaria.

## Variaveis Importantes

Nunca commitar valores reais em `.env`, workflows ou documentacao publica.

- `GOOGLE_CLIENT_ID` / `VITE_GOOGLE_CLIENT_ID`
- `VITE_API_URL` ou `VITE_API_BASE_URL`
- `VITE_META_ADMIN_EMAILS`
- `VITE_CONTACT_FORM_ENDPOINT`
- `QUARKUS_DATASOURCE_JDBC_URL`
- `QUARKUS_DATASOURCE_USERNAME`
- `QUARKUS_DATASOURCE_PASSWORD`
- `TOPDECK_API_KEY`
- `TOPDECK_API_KEY`
- `META_ADMIN_EMAILS`
- `SECURITY_ADMIN_SUBJECTS`
- `STAGING_BEARER_TOKEN`
- `BACKEND_DEPLOY_HOOK_URL`

## Documentacao de Apoio

- Contexto do produto: `PROJECT_CONTEXT.md`
- Regras de agentes: `AGENTS.md` e `.github/agents/AGENTS.md`
- Inventario de skills Codex: `docs/codex-skills.md`
- Deploy: `docs/production-deploy.md`
- Schema PostgreSQL: `docs/postgresql-schema.md`
