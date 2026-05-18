# Changelog

Todas as mudancas notaveis deste projeto serao documentadas aqui.

O formato segue Keep a Changelog e Semantic Versioning. Em CI/CD, este arquivo e atualizado a partir das tags Git e mensagens de commit da release.

## [0.0.0-local] - 2026-05-18

### Added
- API publica `GET /decks/public` e modo de consulta `GET /decks/{id}/consult` com respostas sanitizadas para decks publicos.
- Campo `visibility` (`private`/`public`) em criacao, importacao, atualizacao e resposta de decks, mantendo `private` como padrao compativel.
- Endpoints autenticados `GET /users/me/export` e `DELETE /users/me` para exportacao LGPD e exclusao de dados do usuario.
- Politica de Privacidade publica com dados coletados, finalidade, base legal provavel, retencao, compartilhamento com Google, direitos do titular e contato.
- Footer discreto com versao do frontend.
- Tela Sobre com detalhes do produto, frontend e API.
- Endpoint publico para metadados da API.

### Changed
- Vitrine de decks publicos passa a listar os 10 decks mais recentes e inclui filtro por nome de comandante com busca ao digitar.
- Lista do deck no editor recebeu filtro por nome, filtro por tipo e agrupamento por terrenos, criaturas, artefatos, encantamentos, planeswalkers e demais tipos.
- Cards de recomendacao exibem preview hoveravel das cartas sugeridas para adicionar e remover.
- Manual operacional de agentes documenta validacao Playwright MCP com Vite local, base path, mocks de login/API e checklist UX.
- Documentadas diretrizes canonicas para manter markdowns de regras atualizados, claros e reutilizaveis em novos pedidos via GPT/Codex.
- Frontend recebeu polimento de UX em pt-BR, foco de teclado, alvos de toque, header, stepper e acoes mobile.
- Recomendacoes estrategicas usam diagnostico por arquetipo/bracket e protegem finishers, combos e payoffs contra trocas incoerentes.
- Recomendacoes passaram a respeitar melhor identidade de cor, meta local, combos quase presentes e fallback high-power/cEDH.
- Cadastro, edicao, importacao, analise e recomendacoes ficaram mais compactos e focados nos sinais acionaveis do deck.

### Fixed
- Atualizacao de visibilidade de decks legados deixa de revalidar cartas inalteradas na Scryfall, evitando timeout transacional e falha ao publicar decks antigos.
- Atualizacao de deck passa a reaproveitar linhas de cartas existentes, evitando violacao do indice unico de `deck_cards` no PostgreSQL.
- Auditorias de recomendacao passam a mapear JSONs persistidos como `TEXT` comum no Hibernate, evitando erro de Large Object nos testes PostgreSQL do CI.

### Security
- Fluxo Google documentado para escopos minimos `openid`, `email` e `profile`, sem persistencia de `access_token` ou `refresh_token`.
- Logs de fluxos autenticados deixam de registrar payloads de decks, nomes de cartas em validacoes e detalhes de troca de recomendacao.
