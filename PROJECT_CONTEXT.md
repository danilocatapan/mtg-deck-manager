# MTG Deck Manager - Contexto do Projeto

## Resumo

MTG Deck Manager e uma aplicacao para gerenciar, importar, analisar e melhorar decks de Magic: The Gathering, com foco em Commander. O sistema permite buscar cartas via Scryfall, criar e editar decks, importar decklists, exportar listas em texto, analisar a composicao do deck e gerar recomendacoes de adds/cuts com heuristicas deterministicas, sinergia e dados de meta.

O projeto e dividido em dois apps:

- `backend/`: API REST em Java/Quarkus.
- `frontend/`: SPA em React/Vite.

## Stack

- Backend: Java 25, Quarkus 3.35.x.
- Persistencia: Hibernate ORM + Panache, H2 para dev/test.
- API externa: Scryfall via MicroProfile REST Client.
- Frontend: React 19 + Vite 8.
- Build/CI: Maven Wrapper, npm, GitHub Actions.
- Documentacao API: SmallRye OpenAPI/Swagger.
- Seguranca/config transversal: Quarkus OIDC, headers de seguranca, CORS e mappers de erro.

## Arquitetura

### Backend

A API segue uma organizacao em camadas:

- `controller`: endpoints REST e contratos HTTP.
- `dto`: payloads de entrada/saida expostos pela API.
- `service`: regras de negocio, recomendacoes, analise, importacao e integracoes.
- `service/meta`: carregamento, normalizacao e consulta de dados de meta.
- `service/synergy`: classificacao/tagging e motor de sinergia.
- `repository` e `model`: persistencia Panache/Hibernate.
- `client`: cliente Scryfall.
- `config`: filtros, headers, mappers de erro e configuracoes transversais.

Controllers devem ficar finos. Regra de negocio deve viver em services/componentes testaveis.

### Frontend

O frontend e uma SPA React estruturada em:

- `src/pages`: telas principais.
- `src/components`: componentes de UI e composicao.
- `src/components/ui`: elementos reutilizaveis simples.
- `src/components/layout`: layout da aplicacao.
- `src/services`: integracao com backend, especialmente `api.js`.
- `src/assets` e `public`: imagens, favicon e icones.
- `src/styles/global.css` e `src/index.css`: base visual e estilos globais.

O app usa estado local React e ainda nao possui router dedicado.

## Funcionalidades

- Busca de cartas no Scryfall.
- Busca em lote via collection API do Scryfall.
- CRUD de decks.
- Importacao de decklist em texto.
- Exportacao de deck em formato `quantity name`.
- Analise deterministica do deck: curva de mana, media de CMC, roles como ramp/draw/removal e resumo de composicao.
- Recomendacoes heuristicas com adds/cuts.
- Recomendacoes estrategicas com justificativas mais orientadas por papel, sinergia e meta.
- Carregamento de dataset local de meta por commander.
- Consulta/sincronizacao de fontes de meta.
- UI para listar, criar, editar, importar, analisar e recomendar melhorias em decks.

## Endpoints principais

### Cards

- `GET /cards?name=`: pesquisa cartas na Scryfall.
- `POST /cards/collection`: busca cartas em lote.

### Decks

- `GET /decks`: lista decks.
- `POST /decks`: cria deck.
- `POST /decks/import`: importa decklist.
- `GET /decks/{id}`: obtem deck por id.
- `PUT /decks/{id}`: atualiza deck.
- `DELETE /decks/{id}`: remove deck.
- `GET /decks/{id}/export`: exporta deck como `text/plain`.
- `GET /decks/{id}/analysis`: retorna analise do deck.
- `POST /decks/{id}/recommendations`: gera recomendacoes heuristicas.
- `POST /decks/{id}/recommendations/strategic`: gera recomendacoes estrategicas.

### Meta

- `GET /meta/sources`: lista status das fontes de meta.
- `POST /meta/sync`: sincroniza fontes de meta.
- `GET /meta/commanders/{commander}`: retorna perfil de meta para um commander.

## Como funciona o fluxo de recomendacao

O pipeline de recomendacao combina regras deterministicas, dados de cartas, sinergia e meta:

1. O deck e carregado e normalizado.
2. O commander e a color identity delimitam o espaco de cartas validas.
3. Candidatos sao coletados a partir de dados de meta, heuristicas e/ou cartas conhecidas.
4. Candidatos duplicados ou fora da color identity sao removidos.
5. O deck e analisado por roles, curva e necessidades.
6. O scorer combina sinais como papel no deck, sinergia, meta e adequacao estrategica.
7. Adds sao ranqueados.
8. O deck pode ser completado ate o alvo Commander.
9. Cuts sao sugeridos sem remover comandante e sem quebrar invariantes.
10. A API retorna sugestoes com justificativas.

Invariantes importantes:

- Deck Commander deve respeitar color identity.
- Nao sugerir carta que ja existe no deck como add.
- Nao sugerir corte do comandante.
- Fluxos de completar deck devem mirar 99 cartas + commander = 100 quando aplicavel.

## Dados de meta

O backend possui dataset local em:

- `backend/src/main/resources/meta_dataset.json`
- `backend/src/main/resources/meta/commanders/`

Os componentes em `backend/src/main/java/com/mtg/service/meta` carregam, normalizam e agregam esses dados. A ideia e usar o meta como grounding deterministico para recomendacoes, sem depender de modelo generativo.

## Testes e validacao

Backend:

- Testes com JUnit, Quarkus Test, Mockito e RestAssured.
- Rodar em `backend`: `./mvnw.cmd test` no Windows ou `./mvnw test` no Linux/macOS.

Frontend:

- O projeto ainda nao possui script de testes frontend.
- Rodar em `frontend`: `npm run lint` e `npm run build`.

Validacao manual recomendada para mudancas relevantes:

- Criar ou importar um deck.
- Buscar cartas.
- Abrir editor do deck.
- Rodar analise.
- Gerar recomendacoes e conferir duplicatas, color identity e quantidade.

## Execucao local

Backend no Windows/PowerShell:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd backend
./mvnw.cmd quarkus:dev
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

URLs comuns:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Quarkus Dev UI: `http://localhost:8080/q/dev/`
- Swagger/OpenAPI: rota padrao do SmallRye OpenAPI no Quarkus.

## CI/CD

O workflow `.github/workflows/ci.yml` roda em `master`, pull requests para `master` e execucao manual:

- Backend: Java 25 + `./mvnw test`.
- Frontend: Node 24 + `npm ci` + `npm run build`.
- Publicacao de imagem backend no GHCR em push/manual.
- Deploy backend via hook opcional `BACKEND_DEPLOY_HOOK_URL`.
- Deploy frontend no GitHub Pages usando `VITE_API_URL`.

## Regras de manutencao

- Antes de alterar comportamento observavel, consulte `AGENTS.md` na raiz e `.github/agents/AGENTS.md`.
- Nao alterar regra negocial, contrato REST, criterio de score ou fluxo de recomendacao sem pedido explicito.
- Ao mexer no backend, preservar separacao controller -> service -> repository/client.
- Ao mexer no frontend, centralizar chamadas REST em `src/services/api.js`.
- Sempre adicionar ou atualizar testes quando introduzir regra de negocio.
- Nunca expor segredos no repositorio; usar variaveis de ambiente/configuracao.
- Manter este arquivo atualizado quando mudarem stack, endpoints, arquitetura, CI/CD ou fluxos principais.
