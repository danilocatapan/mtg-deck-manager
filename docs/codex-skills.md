# Codex Skills do Projeto

Ultima atualizacao: 2026-05-22

Este documento registra as skills instaladas no ambiente Codex usado neste projeto e explica quando aciona-las. As skills ficam fora do repositorio, em `C:\Users\danilo.catapan\.codex\skills`; este arquivo existe para dar visibilidade ao time e evitar reinstalacoes ou escolhas repetidas.

Depois de instalar ou atualizar skills, reinicie o Codex para recarregar os metadados.

## Preciso decorar os nomes?

Nao. A regra do projeto em `.github/agents/AGENTS.md` orienta o Codex a revisar este inventario em pedidos nao triviais e escolher skills que melhorem o resultado. Voce pode escrever o pedido em linguagem natural.

Use nomes de skills apenas quando quiser direcionar explicitamente. Caso contrario, diga o resultado esperado:

```text
Melhore a tela de recomendacoes e valide no navegador.
```

Codex deve considerar `frontend-design`, `playwright` e `webapp-testing`.

```text
Quero evoluir o algoritmo de recomendacao sem quebrar color identity nem duplicatas.
```

Codex deve considerar `define-goal`, `create-plan`, `jupyter-notebook` e `codebase-recon`.

```text
O CI falhou, veja o que aconteceu e corrija.
```

Codex deve considerar `gh-fix-ci`.

```text
Prepare o changelog desta entrega.
```

Codex deve considerar `changelog-generator` e `stop-slop`.

## Fontes analisadas

- `openai/skills`: catalogo oficial de skills curated/system.
- Artigo Medium "9 Must-Have Skills for Codex in 2026".
- Reddit `r/codex`: "Top 10 Open-Source Codex Skills".
- `ComposioHQ/awesome-codex-skills`.
- MCP Market: ranking de Agent Skills.

## Skills instaladas

| Skill | Origem | Quando usar neste projeto | Ganho esperado |
| --- | --- | --- | --- |
| `create-plan` | `ComposioHQ/awesome-codex-skills` | Quando o pedido exigir plano antes de editar codigo, principalmente em backend, frontend, recomendacoes ou migrations. | Reduz desvio de escopo e explicita arquivos, riscos e validacoes antes da implementacao. |
| `define-goal` | `openai/skills` | Quando a demanda for ampla, como melhorar recomendacoes, UX ou qualidade do backend, e precisar virar objetivo mensuravel. | Transforma intencoes vagas em criterios de conclusao verificaveis. |
| `codebase-recon` | `yujiachen-y/codebase-recon-skill` | Antes de mexer em areas grandes ou pouco recentes, para mapear hotspots, bug magnets, bus factor e momentum via Git. | Ajuda a priorizar leitura e testes em arquivos mais arriscados. |
| `jupyter-notebook` | `openai/skills` | Para criar notebooks de experimento sobre meta, scoring, ablation de heuristicas, curvas, roles e qualidade de adds/cuts. | Da um formato reproduzivel para evoluir algoritmos de recomendacao sem alterar runtime da API. |
| `frontend-design` | `vipulgupta2048/codex-skills` | Ao criar ou refinar telas React/Vite com impacto visual: recomendacoes, vitrine publica, meta admin, importacao e analise. | Melhora direcao visual, hierarquia, responsividade e acessibilidade sem cair em UI generica. |
| `playwright` | `openai/skills` | Para validar UX no navegador, fluxos anonimos/autenticados com mocks, screenshots e bugs de interacao. | Aproxima a validacao do uso real da SPA e complementa `npm run lint`/`npm run build`. |
| `webapp-testing` | `ComposioHQ/awesome-codex-skills` | Para escrever scripts Playwright pequenos e repetiveis, com servidor local gerenciado, quando a validacao precisar virar automacao. | Facilita testes focados de fluxo e captura de console/DOM sem poluir contexto. |
| `screenshot` | `openai/skills` | Quando for preciso capturar tela, janela ou regiao do sistema fora do alcance do Playwright. | Ajuda em comparacoes visuais e diagnostico de UI local. |
| `gh-fix-ci` | `openai/skills` | Quando CI do GitHub Actions falhar em Maven, PostgreSQL, lint/build frontend, Docker ou deploy. | Encurta diagnostico de logs e direciona correcoes com base nos checks reais. |
| `gh-address-comments` | `openai/skills` | Para resolver comentarios de review em PRs, agrupando feedback e aplicando correcoes com contexto. | Reduz troca manual entre GitHub, arquivos e testes. |
| `changelog-generator` | `ComposioHQ/awesome-codex-skills` | Para transformar commits/resumos em entradas claras de `CHANGELOG.md` e release notes curadas. | Alinha o historico tecnico com linguagem util para usuario, suporte e rastreabilidade. |
| `stop-slop` | `hardikpandya/stop-slop` | Ao revisar README, changelog, release notes, textos de UI e mensagens publicas. | Remove padroes artificiais de escrita e deixa a comunicacao mais direta. |
| `security-threat-model` | `openai/skills` | Para threat modeling de API, auth Google, decks publicos, admin de meta, privacidade e logs. | Explicita ativos, fronteiras de confianca, abuso possivel e mitigacoes. |
| `security-best-practices` | `openai/skills` | Em revisoes de seguranca frontend/web e ao escrever codigo seguro por padrao. | Complementa as regras locais de privacidade, logs e tokens. |

## Como usar

As skills disparam pelo texto do pedido. Tambem e valido citar o nome da skill quando quiser forcar consideracao.

Exemplos:

```text
Use create-plan para planejar a extracao do pipeline de recomendacao sem alterar regra negocial.
```

```text
Use codebase-recon para identificar hotspots antes de mexer em StrategicRecommendationService.
```

```text
Use jupyter-notebook para montar um experimento de score com decks de referencia.
```

```text
Use playwright/webapp-testing para validar a tela de recomendacoes em desktop e mobile.
```

```text
Use changelog-generator e stop-slop para revisar a entrada do CHANGELOG desta entrega.
```

## Mapa por objetivo

### Backend Quarkus

- `create-plan`: planejar endpoints, services e migrations.
- `codebase-recon`: achar arquivos de alto risco antes de refatorar.
- `gh-fix-ci`: corrigir falhas de Maven, PostgreSQL e CI.
- `security-threat-model`: revisar auth, privacidade, DTOs publicos e logs.

### Frontend React/Vite e UX

- `frontend-design`: orientar telas mais claras, acessiveis e visualmente melhores.
- `playwright`: validar interacao real, layout, responsividade e estados.
- `webapp-testing`: automatizar fluxos pequenos com Playwright.
- `screenshot`: capturar comparacoes visuais pontuais.

### Algoritmos de recomendacao

- `define-goal`: definir metrica, corpus e criterio de sucesso.
- `create-plan`: separar descoberta, mudanca, testes e riscos.
- `jupyter-notebook`: testar ideias offline com datasets e exemplos.
- `codebase-recon`: localizar hotspots e historico antes de mexer no pipeline.

### Documentacao, PRs e release

- `changelog-generator`: preparar changelog/release notes.
- `stop-slop`: polir texto para ficar natural e direto.
- `gh-address-comments`: responder feedback de review.
- `gh-fix-ci`: resolver checks quebrados.

## Candidatos analisados e nao instalados

- Reddit: `cook-the-blog`, `yc-intent-radar-skill`, `stargazer`, `meta-ads-skill`, `google-trends-api-skills`, `blog-cover-image-cli`, `luma-attendees-scraper` e `twitter-GTM-find-skill` foram descartados por foco em marketing, scraping, ads ou coleta de contatos fora do objetivo atual do produto.
- Reddit: `svg-animations` foi descartado por enquanto; a UX do projeto deve evoluir com componentes React, Playwright e diretrizes locais antes de adicionar animacoes SVG especializadas.
- Reddit/MCP Market: skills de automacao agressiva de issues/PRs com subagentes foram descartadas por serem amplas demais para o fluxo atual e por exigirem mais governanca de branch/PR.
- MCP Market: `React Code Fix & Linter` foi considerado redundante, pois o projeto ja padroniza `npm run lint` e `npm run build`, e `webapp-testing` cobre validacao browser.
- MCP Market: `GitHub Integration` foi considerado redundante com as skills/plugins GitHub ja disponiveis (`gh-fix-ci`, `gh-address-comments`, GitHub plugin).
- Composio: `issue-triage`, `connect`, `connect-apps`, `datadog-logs`, `sentry-triage`, `paperjsx`, `theme-factory`, `canvas-design`, `mcp-builder` e skills de reuniao/Notion/Slack foram deixadas fora por dependerem de ferramentas externas nao configuradas ou por nao atacarem os hotspots atuais.

## Proximas candidatas

- Criar uma skill local `mtg-recommendation-eval` quando tivermos um protocolo estavel de avaliacao offline para color identity, duplicatas, 99+comandante, cortes e grounding de meta.
- Criar uma skill local `mtg-release-notes` se o processo de `CHANGELOG.md` e `frontend/public/release-notes.json` ficar repetitivo o bastante para merecer checklist proprio.
- Avaliar MCPs com API key, como pesquisa especializada, somente quando houver necessidade concreta de fontes externas para meta, benchmark ou documentacao viva.
