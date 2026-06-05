# AGENTS.md - Regras canonicas de agentes

Versao: agents-2026-06-05
Ultima atualizacao: 2026-06-05

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
- 1 snapshot operacional em `PROJECT_CONTEXT.md`, usado para entender objetivo, stack, contratos e fluxos sem reabrir todo o codigo.

Nao criar resumos paralelos com regras globais. Quando o projeto mudar, atualizar este arquivo e os guias especializados afetados.

Fluxo minimo obrigatorio
------------------------
1. Leia este `AGENTS.md`.
2. Em toda tarefa nao trivial, leia `PROJECT_CONTEXT.md` como snapshot operacional antes de explorar o codigo.
3. Escolha o guia especializado relevante.
4. Antes de alterar comportamento observavel, execute a analise minima definida em `analysis.md`.
5. Reabra codigo alem dos pontos indicados no snapshot somente para confirmar o escopo diretamente afetado, investigar divergencia ou preencher uma lacuna marcada como parcial/planejada.

Economia de contexto para agentes
---------------------------------
- Comece por `rg`/`rg --files` e abra apenas arquivos necessarios ao pedido atual.
- Use primeiro o estado verificado, as prioridades e o mapa rapido de `PROJECT_CONTEXT.md`; nao refaca descoberta ampla quando o snapshot responder ao pedido.
- Para backend, confirme primeiro controller/service/test afetado antes de abrir DTOs, entidades ou migrations.
- Para frontend, confirme primeiro page/component/service afetado antes de abrir CSS/assets.
- Evite colar arquivos inteiros na resposta final; informe arquivos alterados, validacoes executadas e riscos restantes.
- Se a tarefa for documental, compare markdowns com codigo/configuracao real antes de atualizar instrucoes.
- Quando a informacao ja estiver consolidada neste arquivo ou em `PROJECT_CONTEXT.md`, prefira citar o snapshot em vez de reexplorar tudo.

Manual produtivo para Codex
---------------------------
Estas praticas consolidam o fluxo atual do projeto com boas praticas oficiais do Codex e com o material revisado em 2026-05-22 sobre projetos, prompts, tools, approvals, contexto, `AGENTS.md`, MCP/plugins, skills, subagentes e automacoes.

Prompts e escopo:
- Para pedidos complexos, estruturar a demanda em objetivo, contexto, restricoes e criterio de conclusao. Quando o usuario nao fornecer esses itens, inferir pelo repositorio e explicitar as premissas.
- Usar referencias diretas a arquivos, erros, endpoints, telas ou comandos quando existirem. Evitar prompts longos com regras duraveis; regras recorrentes devem virar atualizacao deste arquivo, guia especializado ou skill.
- Quando o pedido estiver ambiguo e puder afetar contrato, regra negocial, seguranca ou dados, fazer perguntas curtas antes de implementar.

Planejamento e threads:
- Para tarefas grandes, ambiguidade alta, investigacoes ou mudancas multi-area, planejar antes de editar e manter uma lista curta de passos.
- Manter uma thread por unidade coerente de trabalho. Usar `/compact` ou resumo operacional quando a conversa ficar longa, e `/resume`/fork apenas quando o trabalho continuar ou ramificar de verdade.
- Ao final de cada tarefa relevante, registrar arquivos alterados, validacoes executadas, riscos restantes e proximos passos objetivos.

Tools, approvals e configuracao:
- Usar tools para ler, editar, testar e validar o resultado em vez de depender so de explicacao. Comecar por comandos read-only e pedir aprovacao para escrita, rede, comandos destrutivos, instalacoes ou operacoes fora do sandbox.
- Manter sandbox e approvals conservadores por padrao. Afrouxar permissoes somente para repositorios confiaveis e fluxos bem entendidos.
- Configuracao pessoal deve ficar em `~/.codex/config.toml`; configuracao compartilhada do projeto deve ficar em `.codex/config.toml`; overrides de CLI devem ser temporarios.
- Selecionar esforco de raciocinio conforme risco: baixo para tarefas mecanicas pequenas, medio/alto para debugging e mudancas compartilhadas, extra alto para arquitetura, seguranca, migracoes ou investigacoes longas.

Reuso, MCP, skills e automacoes:
- Usar MCP/plugins quando o contexto necessario estiver fora do repositorio, mudar com frequencia ou exigir uma ferramenta repetivel, como GitHub, Linear, navegador ou documentacao oficial.
- Antes de executar pedidos nao triviais, revisar rapidamente `docs/codex-skills.md` e decidir se alguma skill instalada melhora o resultado. Se uma skill for aplicavel, use-a ou mencione no andamento qual skill sera usada e por que. Se nenhuma ajudar, siga o fluxo normal sem exigir que o usuario cite nomes de skills.
- Para pedidos amplos ou vagos, reformular mentalmente o prompt usando as skills disponiveis: objetivo mensuravel (`define-goal`), plano (`create-plan`), risco de codigo (`codebase-recon`), UX/browser (`frontend-design`, `playwright`, `webapp-testing`), recomendacoes offline (`jupyter-notebook`), release/documentacao (`changelog-generator`, `stop-slop`) e seguranca (`security-threat-model`, `security-best-practices`).
- Nao obrigar o usuario a decorar skills. Quando o usuario pedir algo como "melhore", "analise", "valide", "arrume CI", "revise UX", "evolua recomendacoes" ou "prepare release", mapear o pedido para a skill adequada a partir do inventario.
- Nao abrir `docs/codex-skills.md` para comandos triviais, respostas conceituais curtas ou tarefas em que a skill ja esteja claramente indicada pelo usuario ou pelo contexto.
- Quando um prompt ou checklist for reutilizado varias vezes, transformar em skill local ou guia especializado antes de virar automacao.
- Criar automacoes apenas para fluxos ja confiaveis manualmente, como resumo de commits, triagem de CI, varredura de bugs, preparacao de release notes ou revisao periodica de instrucoes.
- Se o usuario pedir subagentes ou trabalho paralelo, delegar tarefas independentes com escopo claro, arquivos de responsabilidade e resultado esperado. Nao usar subagentes para trabalho que bloqueia a proxima acao imediata.

Validacao e revisao:
- Codex deve fechar o ciclo: implementar, testar, revisar diff e confirmar que o comportamento pedido foi atendido.
- Usar `/review` ou postura de code review para revisar PRs, commits ou mudancas locais quando o usuario pedir revisao.
- Se uma mesma falha de instrucao ocorrer duas vezes, propor uma retrospectiva curta e atualizar `AGENTS.md`, guia especializado ou skill correspondente.

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
- No fluxo de recomendacao, evitar chamadas externas diretas; ingestao/cache/sync de meta deve evoluir dentro da estrutura existente antes de abrir nova arquitetura.

Congelamento de regra negocial
------------------------------
Qualquer mudanca que possa impactar o comportamento esperado pelo usuario (por ex., remocao de validacao, alteracao de calculo, mudanca de contrato REST) deve ser tratada como alteracao negocial e precisa de aprovacao explicita.

Snapshot do projeto (contexto rapido)
-------------------------------------
- Stack principal: Java 25 / Quarkus 3.35.2 (backend).
- Frontend: Vite 8 + React 19 (separado em `frontend/`, base path `/mtg-deck-manager/`).
- Persistencia: Hibernate ORM / Panache, H2 para `%dev`/`%test`, PostgreSQL/Flyway para `%pg` e `%prod`.
- Integracoes: Scryfall REST, TopDeck.gg como unica fonte externa viva de meta, dataset local de fallback em `backend/src/main/resources/meta`, regras Commander em `backend/src/main/resources/rules`.
- Autenticacao: Google OIDC via ID token Bearer; frontend guarda sessao apenas em `sessionStorage`.
- Privacidade: decks privados por padrao, DTOs publicos sanitizados, exportacao/exclusao LGPD em `/users/me`.
- CI/CD: GitHub Actions em `.github/workflows/ci.yml`, com testes backend H2, testes backend PostgreSQL, lint/build frontend, imagem backend GHCR, deploy backend por hook opcional, smoke tests manuais e deploy GitHub Pages.

Hotspots obrigatorios do dominio
--------------------------------
- Recommendation engine (pipeline: meta -> candidates -> score -> completer -> cuts).
- Strategic recommendations (`StrategicRecommendationService`) e recomendacoes heuristicas (`RecommendationService`).
- Auditoria/aplicacao de recomendacoes (`RecommendationAuditService`, apply/undo swap, feedback).
- Meta dataset ingestion (`MetaDatasetLoader`, `MetaProviderImpl`, `MetaDatasetService`, `ExternalMetaIngestionJob`, `TopDeckMetaAdapter`).
- Color identity / Commander rules (`ColorIdentityMatcher`, `DeckCompleter`, selectors de add/cut).
- Importacao de deck (`DeckImportService`) e normalizacao de listas.
- Decks publicos, likes e copia (`PublicDeckController`, `DeckLikeRepository`, DTOs publicos).
- LGPD/exportacao/exclusao (`UserPrivacyController`, `UserPrivacyService`).
- Seguranca operacional (`SecurityResource`, `SecurityStatusService`, filtros/log sanitization em `config`).

Invariantes globais do dominio
------------------------------
- Decks Commander devem respeitar color identity; nada fora das cores.
- Recomendacao/complemento deve mirar 99 cartas no deck + comandante = 100 quando o fluxo for Commander completo.
- Nao introduzir cartas que ja existem no deck como sugestao de add.
- Sugestoes de corte nao devem remover comandante nem quebrar contratos de quantidade.
- Decks privados nunca devem aparecer em listagens publicas nem ser consultados por anonimos/outros usuarios; expose somente DTOs publicos sanitizados para decks publicos.
- Apply/undo de troca recomendada deve registrar auditoria suficiente para rastreabilidade sem gravar tokens, PII desnecessaria ou payloads sensiveis em logs.
- Meta top decks so deve influenciar recomendacoes quando a amostra minima e os filtros de formato/bracket/fonte forem respeitados.

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

Manual produtivo para Playwright MCP
------------------------------------
Use este fluxo quando o pedido exigir validar UX/frontend no navegador. O objetivo e evitar retrabalho com Vite, base path e login Google.

1. Antes de abrir o browser, rode as validacoes estaticas do frontend quando aplicavel: em `frontend`, execute `npm run lint` e `npm run build`.
2. No Windows/PowerShell, suba o Vite explicitamente com `npm.cmd`, nao com `npm` via `Start-Process`, pois a associacao do Windows pode abrir o executavel errado:

```powershell
Start-Process -FilePath npm.cmd -ArgumentList 'run','dev','--','--host','127.0.0.1','--port','5173' -WorkingDirectory 'C:\Users\danilo.catapan\Documents\mtg-deck-manager\frontend' -WindowStyle Hidden -PassThru
```

3. Verifique a URL antes do Playwright:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:5173/mtg-deck-manager/
```

4. Abra sempre a URL com base path do Vite: `http://localhost:5173/mtg-deck-manager/`.
5. Se a porta `5173` estiver ocupada, use outra porta e mantenha o path `/mtg-deck-manager/`.
6. Para encerrar servidor local iniciado pelo agente, use `Stop-Process -Id [PID]` somente no PID retornado pelo `Start-Process`.

Execucao Playwright no Windows/PowerShell:
- Antes de tentar automacao, confirme `npx.cmd -y playwright --version` ou `npx.cmd -y @playwright/test --version`. Nao perca tempo tentando `python -c "import playwright"` se o pacote Python nao estiver instalado neste ambiente.
- Quando usar `webapp-testing/scripts/with_server.py`, passe executaveis Windows explicitamente: `npm.cmd` para subir Vite e `npx.cmd` para rodar Playwright. O helper pode nao resolver `npm`/`npx` sem `.cmd`.
- Para specs temporarias sem `playwright.config.*`, rode `npx.cmd -y @playwright/test test [spec] --browser=chromium --reporter=line`. Nao use `--project=chromium` sem config, porque nao existe projeto nomeado por padrao.
- Para scripts avulsos em Node, nao dependa de `npx -p playwright node -e "import ... from 'playwright'"`; neste ambiente o pacote pode ficar disponivel como CLI, mas nao como modulo resolvivel pelo `node` do repositorio.
- Para fluxos autenticados com mocks, prefira uma spec temporaria do `@playwright/test` com `page.route`, `page.addInitScript` e `sessionStorage`, mantendo a URL real `http://127.0.0.1:[porta]/mtg-deck-manager/`.

Estrategia de validacao com login/API:
- Primeiro valide o estado real anonimo sem mocks: carregamento, chamada de login, tela publica, mensagens de API indisponivel/iniciando e layout.
- Nao tente completar login Google real no Playwright sem token/credencial fornecida explicitamente. O login depende de Google Identity Services e normalmente bloqueia automacao/local sem configuracao real.
- Para validar fluxos autenticados, use mocks de rede no Playwright MCP (`browser_run_code_unsafe` + `page.route`) com respostas equivalentes aos contratos REST. Simule `sessionStorage` com `mtg_google_id_token` e `mtg_google_profile` apenas quando o teste precisar liberar UI autenticada; o token fake precisa ter payload JWT com `sub`, `exp` futuro e, quando `VITE_GOOGLE_CLIENT_ID` estiver configurado, `aud` correspondente para passar por `getAuthToken()`.
- Use mocks para `GET /public/decks`, `GET /decks/public` quando validar compatibilidade antiga, `GET /decks`, `GET /decks/{id}/consult`, create/update/import/delete e `POST /cards/collection`. Nao use chamadas reais a Scryfall para validar UX.
- Contratos REST e autorizacao real devem ser validados por testes backend (`./mvnw.cmd test`), nao por login manual no browser.

Checklist UX no Playwright:
- Desktop e mobile: rode pelo menos `browser_resize` em 1440x1000 e 390x844 quando a mudanca afetar layout principal.
- Use `browser_snapshot` para acessibilidade/navegacao e screenshot apenas para evidencia visual; se `fullPage` travar, use screenshot de viewport.
- Verifique que textos nao sobrepoem botoes/cards, estados vazio/loading/erro aparecem, botoes desabilitados comunicam motivo, foco/navegacao por roles continuam claros e a acao primaria da tela fica evidente.
- Para novos fluxos, validar caminho feliz, estado vazio, erro de API e permissao/autenticacao. Para fluxos antigos, validar que criar/importar/editar/excluir/analisar continuam acessiveis quando autenticado.

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
- Backend (Windows / PowerShell): sempre execute comandos Maven locais no mesmo bloco/comando que define `JAVA_HOME` e atualiza o `Path` com o JDK do desenvolvedor. Nao rode `./mvnw.cmd test` cru no PowerShell, porque o shell pode cair no Java 17 do ambiente e falhar contra classes Java 25:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
./mvnw.cmd test
```

Para testes filtrados, mantenha o mesmo preambulo:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
./mvnw.cmd "-Dtest=DeckRecommendationIntegrationTest" test
```

Endpoints principais atuais
---------------------------
- `GET /` e `GET /app/info`.
- `GET /cards?name=` e `POST /cards/collection`.
- `GET /decks`, `GET /decks/public` (compatibilidade), `POST /decks`, `POST /decks/import`, `GET /decks/{id}`, `GET /decks/{id}/consult`, `PUT /decks/{id}`, `DELETE /decks/{id}`.
- `GET /decks/{id}/export`, `GET /decks/{id}/analysis`, `GET /decks/{id}/legality`, `POST /decks/{id}/comparison`.
- `POST /decks/{id}/recommendations`, `POST /decks/{id}/recommendations/strategic`, `POST /decks/{id}/recommendations/apply-swap`, `POST /decks/{id}/recommendations/undo-swap`.
- `GET /public/decks`, `GET /public/decks/top`, `GET /public/decks/{id}`, `POST /public/decks/{id}/copy`, `POST /public/decks/{id}/like`, `DELETE /public/decks/{id}/like`.
- `GET /meta/sources`, `POST /meta/sync`, `GET /meta/decks`, `POST /meta/rebuild-profiles`, `GET /meta/commanders/{commander}`.
- `POST /meta/external-decks/import`, `POST /meta/combos/sync`; top decks para recomendacao entram exclusivamente pelo sync automatico `POST /meta/sync`.
- `POST /recommendation-audits/{id}/feedback`.
- `GET /users/me/export` e `DELETE /users/me`.
- `POST /security/status/check` para diagnostico read-only de seguranca; exige usuario autenticado com role `admin` ou subject configurado em `SECURITY_ADMIN_SUBJECTS`, nao deve expor secrets/dados pessoais e deve manter logs sem valores sensiveis.
