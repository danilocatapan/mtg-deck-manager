# AGENTS.md - Regras canonicas de agentes

Papel deste arquivo
-------------------
Este e o arquivo canonico com regras globais, prioridades, topologia documental e politicas que orientam agentes, Copilot e Codex neste repositorio.

Decisao de topologia documental
-------------------------------
Adotamos explicitamente:
- 1 ponto de entrada para Codex na raiz (`/AGENTS.md`).
- 1 bootstrap historico para Copilot (`.github/copilot-instructions.md`).
- 1 arquivo canonico (`.github/agents/AGENTS.md`).
- 5 guias especializados (`analysis.md`, `backend-quarkus.md`, `frontend-react.md`, `testing.md`, `workflow-graph.md`).

Nao criar resumos paralelos com regras globais. Quando o projeto mudar, atualizar este arquivo e os guias especializados afetados.

Fluxo minimo obrigatorio
------------------------
1. Leia este `AGENTS.md`.
2. Escolha o guia especializado relevante.
3. Antes de alterar comportamento observavel, execute a analise minima definida em `analysis.md`.

Manutencao de regras e contexto para IA
---------------------------------------
- Quando uma demanda adicionar, remover ou alterar regras da aplicacao, praticas de engenharia, criterios de validacao ou contexto operacional, atualize os arquivos `.md` canonicos afetados antes da resposta final.
- Use este arquivo para regras globais e transversais. Use os guias especializados apenas quando a regra pertencer claramente ao tema do guia (`analysis`, `backend`, `frontend`, `testing` ou `workflow-graph`).
- Nao replique regras globais em `AGENTS.md` da raiz nem em `.github/copilot-instructions.md`; esses arquivos devem continuar como pontos de entrada/bootstrap.
- Ao atualizar markdowns de regras, mantenha secoes curtas, titulos consistentes, linguagem objetiva e contexto suficiente para que novos pedidos via GPT/Codex entendam a pratica atual sem depender de historico de conversa.
- Inclua exemplos somente quando reduzirem ambiguidade. Prefira placeholders explicitos como `[DADO EXEMPLO]` ou `[PLACEHOLDER]` quando o valor real depender do pedido futuro.
- Antes da resposta final, relate em portugues as etapas executadas e a justificativa resumida da alteracao ou da decisao de nao alterar. Forneca raciocinio resumido e auditavel, sem expor cadeia interna de pensamento.
- Ao apresentar o resultado de uma atualizacao de `.md`, comece por `Alteracoes realizadas:` e liste os pontos modificados. Quando o usuario pedir o conteudo revisado, inclua `Conteudo atualizado do arquivo .md:` seguido do conteudo completo do arquivo afetado em markdown puro.

Regras globais inegociaveis
---------------------------
- Sempre validar contratos observaveis (endpoints + testes) antes de alterar comportamento.
- Nao alterar regra negocial, fluxo negocial, criterio de decisao, side effects ou contratos observaveis sem pedido explicito: registre como achado e consulte stakeholders.
- Sempre registrar mudancas relevantes no `CHANGELOG.md` e, quando houver impacto para usuarios, tambem atualizar as release notes publicas em `frontend/public/release-notes.json`.

Release notes e changelog
-------------------------
- Sempre que alterar texto em `CHANGELOG.md` ou `frontend/public/release-notes.json`, atualizar a data da entrada afetada para a data da alteracao.
- Manter as release notes publicas curtas e curadas: em `frontend/public/release-notes.json`, cada categoria deve priorizar apenas os itens mais impactantes para o usuario ou para suporte/rastreabilidade.
- Como limite padrao, usar ate 5 itens por categoria nas release notes publicas. Itens internos, redundantes ou de baixo impacto devem ficar fora da tela publica, mesmo que possam existir no changelog quando forem relevantes para historico tecnico.
- Consolidar mudancas relacionadas em uma unica frase objetiva, evitando listas longas de detalhes de implementacao.

Controle de complexidade
------------------------
- Implementar sempre a solucao mais simples que resolve a necessidade atual.
- Nao criar camadas preventivas como novos adapters, ingestion services, normalizers, persistence abstractions ou DTOs paralelos sem pelo menos uma necessidade concreta imediata.
- Reusar componentes existentes antes de criar pacotes novos; novas abstracoes so entram quando reduzem duplicacao real, isolam uma integracao externa ja usada ou simplificam comportamento observavel.
- Em meta/recomendacoes, manter o runtime sem chamadas externas diretas; ingestao/cache deve evoluir dentro da estrutura existente antes de abrir nova arquitetura.

Congelamento de regra negocial
------------------------------
Qualquer mudanca que possa impactar o comportamento esperado pelo usuario (por ex., remocao de validacao, alteracao de calculo, mudanca de contrato REST) deve ser tratada como alteracao negocial e precisa de aprovacao explicita.

Snapshot do projeto (contexto rapido)
-------------------------------------
- Stack principal: Java 25 / Quarkus 3.35.x (backend).
- Frontend: Vite 8 + React 19 (separado em `frontend/`).
- Persistencia: Hibernate ORM / Panache (H2 para dev/test).
- Integracoes: Scryfall REST, dataset local EDHREC em `backend/src/main/resources/meta`, adapters de meta em `service/meta`.
- CI/CD: GitHub Actions em `.github/workflows/ci.yml`, com testes backend, build frontend, imagem backend e deploy GitHub Pages.

Hotspots obrigatorios do dominio
--------------------------------
- Recommendation engine (pipeline: meta -> candidates -> score -> completer -> cuts).
- Strategic recommendations (`StrategicRecommendationService`) e recomendacoes heuristicas (`RecommendationService`).
- Meta dataset ingestion (`MetaDatasetLoader`, `MetaProviderImpl`, adapters em `service/meta`).
- Color identity / Commander rules (`ColorIdentityMatcher`, `DeckCompleter`, selectors de add/cut).
- Importacao de deck (`DeckImportService`) e normalizacao de listas.

Invariantes globais do dominio
------------------------------
- Decks Commander devem respeitar color identity; nada fora das cores.
- Recomendacao/complemento deve mirar 99 cartas no deck + comandante = 100 quando o fluxo for Commander completo.
- Nao introduzir cartas que ja existem no deck como sugestao de add.
- Sugestoes de corte nao devem remover comandante nem quebrar contratos de quantidade.

Prioridades operacionais transversais
-------------------------------------
- P0: Corrigir violacao de invariantes (cores, tamanho, contratos REST, seguranca).
- P1: Melhorar qualidade de recomendacao (meta grounding, sinergia, cortes estrategicos).
- P2: Refatoracao de infraestrutura (extrair pipeline, performance, cache).

Validacao minima antes de concluir mudanca
------------------------------------------
- Executar testes automatizados relacionados.
- Backend: compilar/testar com Maven wrapper.
- Frontend: rodar lint/build quando alterar UI, services ou assets empacotados.
- Validar um caso manual representativo quando a mudanca afetar recomendacoes, importacao ou UI principal.
- Se mudanca afetar contrato, adicionar testes de contrato/end-to-end ou testes de controller equivalentes.

Guia rapido de roteamento
-------------------------
- Bug/regressao funcional -> `analysis.md` (siga checklist).
- Mudar implementacao de endpoint/service -> `backend-quarkus.md` + `testing.md`.
- Ajuste de algoritmo de recomendacao -> `backend-quarkus.md` + `workflow-graph.md`.
- Mudanca de UI, fluxo de formulario, assets ou cliente HTTP -> `frontend-react.md` + `testing.md`.

Execucao local (desenvolvimento)
--------------------------------
- Frontend: em `frontend`, use `npm run dev` para desenvolvimento.
- Frontend validation: em `frontend`, execute `npm run lint` e `npm run build`.
- Backend (Windows / PowerShell): antes de executar comandos Maven locais, defina `JAVA_HOME` e atualize o `Path` com o JDK do desenvolvedor:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Depois disso, em `backend`, execute `./mvnw.cmd test` ou `./mvnw.cmd clean install` conforme necessario.

Endpoints principais atuais
---------------------------
- `GET /cards?name=` e `POST /cards/collection`.
- `GET /decks`, `POST /decks`, `POST /decks/import`, `GET /decks/{id}`, `PUT /decks/{id}`, `DELETE /decks/{id}`.
- `GET /decks/{id}/export`, `GET /decks/{id}/analysis`, `POST /decks/{id}/recommendations`, `POST /decks/{id}/recommendations/strategic`.
- `GET /meta/sources`, `POST /meta/sync`, `GET /meta/commanders/{commander}`.
