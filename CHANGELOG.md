# Changelog

Todas as mudancas notaveis deste projeto serao documentadas aqui.

O formato segue Keep a Changelog e Semantic Versioning. Em CI/CD, este arquivo e atualizado a partir das tags Git e mensagens de commit da release.

## [0.0.0-local] - 2026-05-22

### Added
- Metadados de impressao nas cartas importadas (`scryfall_id`, edicao, numero de colecao, acabamento e imagem), preservando a arte correta quando listas Moxfield/MTG Arena/Archidekt trazem `(SET) numero` e foil.
- Inventario `docs/codex-skills.md` documentando skills Codex instaladas, ganhos esperados, exemplos de uso e candidatos avaliados para backend, frontend, UX, seguranca, documentacao e recomendacoes.
- Configuracao compartilhada `.codex/config.toml` para padronizar approvals conservadores, sandbox de workspace, busca cacheada, snapshot de shell e multi-agent em sessoes confiaveis do Codex.
- Tela `Meta Admin` no frontend para usuario Google autorizado importar JSON de top decks, consultar snapshots, abrir detalhes e registrar sync manual com orientacoes objetivas sobre impacto nas recomendacoes.
- API administrativa `POST /meta/top-decks/import`, consultas e sync manual para top decks externos rankeados, com upsert idempotente, historico mensal, projecao em decks publicos e atualizacao imediata dos perfis de meta usados nas recomendacoes.
- Avaliacao automatizada dos sinais de top decks garante amostra minima, rastreabilidade `meta_top_decks` e invariantes de recomendacao antes de privilegiar cartas recorrentes.
- Analise de decks passa a expor as cartas por papel em `roleCards`, permitindo detalhar Ramp, Compra, Interacao, Protecao, Limpa-mesa, Vitoria e Terrenos na UI.
- Likes em todos os decks publicos, com voto unico por usuario autenticado e ranking interno por periodo em `GET /public/decks/top`.
- Endpoint administrativo `POST /meta/external-decks/import` para importar decks externos como publicos, com origem marcada e suporte inicial a payload estruturado, MTG Arena, LigaMagic e formato generico.
- Pagina publica de contato para enviar sugestoes, bugs, duvidas de privacidade e feedback aos mantenedores sem exigir login Google.
- API publica `GET /public/decks`, `GET /public/decks/{id}` e `POST /public/decks/{id}/copy` para listar, consultar e copiar decks publicos.
- Campo `visibility` (`private`/`public`) em criacao, importacao, atualizacao e resposta de decks, mantendo `private` como padrao compativel.
- Endpoints autenticados `GET /users/me/export` e `DELETE /users/me` para exportacao LGPD e exclusao de dados do usuario.
- Politica de Privacidade publica com dados coletados, finalidade, base legal provavel, retencao, compartilhamento com Google, direitos do titular e contato.
- Footer discreto com versao do frontend.
- Tela Sobre com detalhes do produto, frontend e API.
- Endpoint publico para metadados da API.
- Endpoint administrativo `POST /security/status/check` para diagnostico read-only de postura de seguranca, com autorizacao por role/subject admin e respostas redigidas.
- Script `tools/security-setup-guide.ps1` para orientar configuracao segura de variaveis, secrets e uso do diagnostico sem exibir valores sensiveis.

### Changed
- Importacao de decks agora normaliza exportacoes com edicao/collector number, ignora sideboard/maybeboard em Commander e resolve cartas em lote pelo identificador de impressao antes de cair para nome.
- Bootstraps de Codex e Copilot foram alinhados ao inventario `docs/codex-skills.md`, deixando claro que skills sao recurso do Codex e que Copilot pode usar o documento como checklist de orientacao.
- Manual canonico de agentes passa a consultar `docs/codex-skills.md` em pedidos nao triviais para selecionar skills automaticamente, sem exigir que o usuario decore nomes ou adapte prompts manualmente.
- Manual canonico de agentes recebeu um fluxo produtivo para Codex cobrindo prompts, planejamento, contexto, approvals, configuracao, MCP/plugins, skills, subagentes, automacoes, validacao e revisao.
- Documentacao operacional e agents foram versionados em `agents-2026-05-21`/`docs-2026-05-21`, alinhando contexto, endpoints, PostgreSQL/Flyway, privacidade, meta top decks e fluxo atual de recomendacoes para reduzir reexploracao e alucinacao em Codex/Copilot.
- Recomendacoes estrategicas passam a usar top decks importados como fonte `meta_top_decks` quando ha amostra suficiente por comandante/bracket, preservando score, filtros de cor, bloqueio de duplicatas e cortes seguros.
- Cards de troca nas recomendacoes podem ser recolhidos ao clicar no card, deixando apenas o resumo com carta que entra e carta removida.
- Telas de listagem, edicao e analise de decks receberam preview de carta reutilizavel, acoes flutuantes padronizadas, galeria com rolagem interna e ajustes mobile para comandante, imagens e footer.
- Tela de consulta "Ver deck" passa a reutilizar a lista do editor, com filtro por nome, filtro por tipo, agrupamento por tipo e alternancia entre lista e imagens em modo somente leitura.
- Vitrine publica exibe contagem/estado de likes e permite curtir ou remover o proprio like em decks publicos.
- UX de importacao, analise e recomendacoes ficou mais visual e compacta: preview progressivo, abas em status/curva/papeis/combos, graficos CSS e cards de troca 1:1 com impacto e risco visiveis.
- Contratos publicos de decks passam a retornar `ownedByCurrentUser` para ocultar a acao de copiar quando o deck publico ja pertence ao usuario autenticado, sem expor `ownerId`.
- Vitrine de decks publicos passa a listar ate 24 decks recentes, inclui filtro por nome de comandante e permite copiar decks publicos para a propria biblioteca como privados.
- Vitrine e biblioteca passam a usar cards visuais com arte do comandante, identidade de cor, autor, copia direta e skeleton loading.
- Fluxo mobile recebeu navegacao rapida fixa para criar, importar e voltar ao topo sem perder contexto em listas longas.
- Lista do deck no editor recebeu filtro por nome, filtro por tipo e agrupamento por terrenos, criaturas, artefatos, encantamentos, planeswalkers e demais tipos.
- Cards de recomendacao exibem preview hoveravel das cartas sugeridas para adicionar e remover.
- Aplicacao de troca recomendada passa a usar dialogo proprio com resumo visual do entra/sai, substituindo confirmacao nativa do navegador.
- Manual operacional de agentes documenta validacao Playwright MCP com Vite local, base path, mocks de login/API e checklist UX.
- Documentadas diretrizes canonicas para manter markdowns de regras atualizados, claros e reutilizaveis em novos pedidos via GPT/Codex.
- Frontend recebeu polimento de UX em pt-BR, foco de teclado, alvos de toque, header, stepper e acoes mobile.
- Recomendacoes estrategicas usam diagnostico por arquetipo/bracket e protegem finishers, combos e payoffs contra trocas incoerentes.
- Recomendacoes passaram a respeitar melhor identidade de cor, meta local, combos quase presentes e fallback high-power/cEDH.
- Cadastro, edicao, importacao, analise e recomendacoes ficaram mais compactos e focados nos sinais acionaveis do deck.
- Exclusao de decks no frontend passa a usar um dialogo proprio, mantendo a experiencia visual consistente.

### Fixed
- Botao de login Google passa a aguardar o carregamento assincrono do script do Google e a CSP permite o estilo oficial do Google Identity, evitando depender de F5 para aparecer.
- Preview de carta deixa de manter falhas temporarias de imagem no cache da sessao e centraliza melhor o estado "Imagem indisponivel" no mobile.
- Nomes de cartas na aba Combos da analise agora usam preview de imagem como nas demais listas.
- Preview de imagem ao tocar/passar sobre nomes de cartas deixa de ficar cortado em telas mobile.
- Tela mobile de edicao deixa de duplicar as acoes Analisar/Recomendacoes no card "Proximo passo" e na barra flutuante.
- Feedback de salvamento no editor deixa de manter "Salvando deck..." depois que a atualizacao termina.
- Header mobile ajustado para evitar clipping do logo em telas estreitas.
- Atualizacao de visibilidade de decks legados deixa de revalidar cartas inalteradas na Scryfall, evitando timeout transacional e falha ao publicar decks antigos.
- Atualizacao de deck passa a reaproveitar linhas de cartas existentes, evitando violacao do indice unico de `deck_cards` no PostgreSQL.
- Auditorias de recomendacao passam a mapear JSONs persistidos como `TEXT` comum no Hibernate, evitando erro de Large Object nos testes PostgreSQL do CI.

### Security
- Endpoints `/meta/top-decks/*` aceitam JWT Google allowlistado por e-mail para uso seguro da tela admin, mantendo `X-Admin-Key` para Swagger e operacao tecnica.
- Formulario de contato usa endpoint externo configuravel sem persistir mensagens no banco da aplicacao, sem anexos e sem envio automatico de tokens ou dados da sessao.
- Fluxo Google documentado para escopos minimos `openid`, `email` e `profile`, sem persistencia de `access_token` ou `refresh_token`.
- Logs de fluxos autenticados deixam de registrar payloads de decks, nomes de cartas em validacoes e detalhes de troca de recomendacao.
- Diagnostico de seguranca passa a registrar logs operacionais sem tokens, secrets, payloads completos, dados pessoais ou detalhes sensiveis de infraestrutura.
- Sessao no frontend passa a validar issuer/audience/exp do ID token, limpar credenciais em 401 e evitar persistencia legada em `localStorage`.
