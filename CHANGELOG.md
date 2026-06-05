# Changelog

Todas as mudancas notaveis deste projeto serao documentadas aqui.

O formato segue Keep a Changelog e Semantic Versioning. Em CI/CD, este arquivo e atualizado a partir das tags Git e mensagens de commit da release.

## [0.0.0-local] - 2026-06-05

### Added
- Benchmark determinístico offline com 20 casos versionados, métricas calculadas, persistência de rodadas/resultados e preservação da última rodada válida.
- Revisão humana cega A/B no Meta Admin com quórum de 3 votos por caso, maioria simples e empate explícito.
- Relatórios agregados de feedback por comandante, bracket, status e motivo, sem expor identidade ou notas privadas.
- Modo diagnóstico frontend opt-in por sessão e logs backend estruturados e sanitizados para auditoria operacional.
- Migration V14 cria persistência de rodadas, resultados por caso e avaliações humanas do benchmark.
- CI PostgreSQL valida o caminho V1-V12 com dados legados, executa V13/V14 e confirma remoção seletiva e preservação dos decks curados.
- Base canonica PostgreSQL `meta_decks`/`meta_deck_cards` para snapshots TopDeck.gg normalizados, com substituicao idempotente por fonte e preservacao do ultimo snapshot valido.
- Meta Admin passa a mostrar proximas acoes derivadas do benchmark, cobertura da fonte e feedback agregado sem checklist manual paralelo.
- Rodadas estrategicas expoem `auditId` e permitem feedback simples `Util`, `Nao util` ou `Precisa revisao`.
- Endpoint admin read-only `GET /meta/recommendation-benchmark/summary` expoe cobertura inicial, metricas-alvo e status do benchmark sistema vs GPT.
- Fixtures seed em `backend/src/test/resources/recommendation-benchmark/` iniciam o corpus com Xenagos, K'rrik, Grand Arbiter e Kess, preparando avaliacao curada sem usar GPT como fonte de verdade.
- Colecao minima por usuario com importacao textual em `POST /users/me/collection/import`, listagem em `GET /users/me/collection` e exportacao/remocao junto aos dados LGPD.
- Quality gate das recomendacoes estrategicas expõe confiança, cobertura, fontes, freshness, limitações e status de benchmark contra GPT antes de afirmar superioridade.
- Protocolo `docs/recommendation-gpt-benchmark.md` define corpus, baseline GPT, métricas e regra de produto para provar superioridade do recomendador Commander.
- Script operacional para migrar PostgreSQL do Render para Neon via Docker, com imagem PostgreSQL configuravel, dump, restore, validacao de contagens e template de variaveis Quarkus.
- Cache local de combos conhecidos com tabelas `meta_combos`/`meta_combo_cards`, adapter Commander Spellbook e endpoint administrativo `POST /meta/combos/sync`.
- Contrato de analise passa a expor `manaCurveCards`, permitindo auditar quais cartas compoem cada ponto da curva de mana.
- Benchmark local para K'rrik, Son of Yawgmoth em cEDH, alinhando recomendacoes offline com sinais de EDHREC/Moxfield/GPT para rituais, tutors, fast mana, Necropotence, reanimacao e protecao.
- Metadados de impressao nas cartas importadas (`scryfall_id`, edicao, numero de colecao, acabamento e imagem), preservando a arte correta quando listas Moxfield/MTG Arena/Archidekt trazem `(SET) numero` e foil.
- Inventario `docs/codex-skills.md` documentando skills Codex instaladas, ganhos esperados, exemplos de uso e candidatos avaliados para backend, frontend, UX, seguranca, documentacao e recomendacoes.
- Configuracao compartilhada `.codex/config.toml` para padronizar approvals conservadores, sandbox de workspace, busca cacheada, snapshot de shell e multi-agent em sessoes confiaveis do Codex.
- API administrativa de meta iniciou suporte a snapshots rankeados; o fluxo manual correspondente foi removido nesta mesma entrega em favor de `POST /meta/sync`.
- Avaliacao automatizada dos sinais do snapshot canonico garante amostra minima e invariantes de recomendacao antes de privilegiar cartas recorrentes.
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
- Fluxos de sucesso do benchmark, revisão cega, feedback agregado e diagnóstico passam a ter validação Playwright obrigatória.
- `POST /meta/sync` passa a executar o fluxo completo TopDeck.gg: buscar, normalizar, persistir e reconstruir perfis por comandante/bracket.
- TopDeck.gg passa a ser a unica fonte externa viva de meta; dataset local permanece como fallback embarcado para cobertura sem operacao manual.
- Meta Admin deixa de importar JSON/decklists e passa a oferecer apenas sincronizacao automatica e acompanhamento operacional.
- `PROJECT_CONTEXT.md` passa a funcionar como snapshot operacional de leitura em 60 segundos, com estado verificado, limites atuais, prioridades, mapa rapido para agentes e validacoes conhecidas; os arquivos `AGENTS.md` agora exigem sua leitura antes de reexplorar tarefas nao triviais.
- `ownedOnly=true` passa a filtrar cartas sugeridas contra a colecao persistida do usuario; sem inventario valido, o quality gate continua `low_confidence` e explica a limitacao.
- Meta Admin troca a acao generica por `Sincronizar TopDeck.gg` e exibe status de fonte, attribution e resumo do benchmark sem expor API key.
- Adaptador TopDeck.gg passa a solicitar `deckObj`/commander, preferir cartas estruturadas quando disponiveis, cair para decklist textual e degradar com cache em 401/403/429.
- Endpoint estrategico de recomendacoes passa a retornar uma execução auditavel com metadados e lista de trocas, e a UI avisa quando faltam dados para superar uma analise GPT ampla.
- Parametros de recomendacao deixam de ser apenas decorativos: orçamento filtra/penaliza cartas caras e `strategy` passa a influenciar o score em modos como budget, power, theme e cEDH.
- Frontend agora documenta baseline WCAG 2.2 AA com AAA oportunista, checklist de teclado/foco/contraste e governanca Playwright/axe para mudancas de UI.
- Testes frontend ganharam scripts `test:e2e` e `test:a11y`, mocks de REST/login Google, validacao axe desktop/mobile e checagem de contraste dos tokens semanticos principais.
- Navegacao por hash foi centralizada em helper local, com rotas compartilhaveis `#/contact`, `#/release-notes`, `#/meta-admin`, `#/import` e `#/public/:id`.
- CI frontend passa a rodar Playwright e axe, e a SPA aceita hash routes internas para editar, analisar e abrir recomendacoes de decks salvos sem adicionar router runtime.
- Cobertura Playwright foi ampliada para mobile e para estados de API iniciando, contato, release notes vazias, Meta Admin, popovers e dialogs por teclado.
- Criacao e importacao exibem uma previa publica quando a visibilidade e `Publico`, explicando vitrine, likes, copia e dados privados que nao sao expostos.
- Acessibilidade do frontend reforcada com dialogs focados, tabs com teclado, disclosures em recomendacoes, popovers de carta acionaveis por foco e labels de carregamento contextuais.
- Importacao e edicao de decks passam a exibir validacao de campos, motivos para a acao desabilitada e microcopy de privacidade para decks publicos.
- Recomendacoes exibem um bloco "Por que esta troca e segura" com invariantes de cor, duplicidade, comandante, bracket, fonte e amostra.
- Aba Curva da analise passa a listar as cartas por custo de mana, com agrupamento visual `7+`, mantendo a mesma soma exibida no grafico.
- Detector de combos inclui o comandante no contexto de analise, recomendacao e protecao de pecas-chave.
- Fallback generico de recomendacoes passa a usar staples por cor, papel e bracket quando nao ha perfil meta suficiente do comandante, sem substituir candidatos meta/arquetipo ja confiaveis.
- Deteccao de arquetipo reconhece melhor Turbo/Combo por reducao de custo phyrexiana e Voltron por comandante focado em combate.
- Tela de recomendacoes deixa de limitar visualmente a lista a 5 cards e passa a renderizar todas as trocas retornadas pelo backend.
- Analise de curva e CMC medio passa a ignorar terrenos e classificar MDFCs pela face principal, evitando que cartas como Agadeem's Awakening sejam agrupadas como terreno.
- Recomendacoes estrategicas passam a mirar 10 trocas por padrao quando houver pares legais suficientes, mantendo filtros de cor, duplicidade e cortes seguros.
- Importacao de decks agora normaliza exportacoes com edicao/collector number, ignora sideboard/maybeboard em Commander e resolve cartas em lote pelo identificador de impressao antes de cair para nome.
- Bootstraps de Codex e Copilot foram alinhados ao inventario `docs/codex-skills.md`, deixando claro que skills sao recurso do Codex e que Copilot pode usar o documento como checklist de orientacao.
- Manual canonico de agentes passa a consultar `docs/codex-skills.md` em pedidos nao triviais para selecionar skills automaticamente, sem exigir que o usuario decore nomes ou adapte prompts manualmente.
- Manual canonico de agentes recebeu um fluxo produtivo para Codex cobrindo prompts, planejamento, contexto, approvals, configuracao, MCP/plugins, skills, subagentes, automacoes, validacao e revisao.
- Documentacao operacional e agents foram versionados em `agents-2026-05-21`/`docs-2026-05-21`, alinhando contexto, endpoints, PostgreSQL/Flyway, privacidade, meta top decks e fluxo atual de recomendacoes para reduzir reexploracao e alucinacao em Codex/Copilot.
- Recomendacoes estrategicas passam a usar o snapshot canonico quando ha amostra suficiente por comandante/bracket, preservando score, filtros de cor, bloqueio de duplicatas e cortes seguros.
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
- Cards de métricas do benchmark deixam de comprimir e cortar amostra/meta no Meta Admin desktop.
- Botao de sincronizacao do Meta Admin deixa de apenas registrar um lote e passa a buscar dados reais do TopDeck.gg.
- Migration de remocao do fluxo manual preserva decks curados por `POST /meta/external-decks/import`, removendo apenas projecoes ligadas aos registros legados.

### Removed
- Endpoints, DTOs, entidades, tabelas e UI de importacao manual de top decks, incluindo projecoes automaticas desses decks na vitrine publica.
- Adapters ativos Spicerack/EDHREC e o placeholder offline de importacao EDHREC.
- Recomendacoes de K'rrik deixam de sugerir remover `Vilis, Broker of Blood` quando ele atua como engine de compra por perda de vida no plano turbo-combo.
- Importacao e preview agora descontam o comandante quando ele aparece dentro da lista, preservando o formato Commander de 99 cartas no main deck mais comandante.
- Terrenos que produzem mana deixam de ser classificados como ramp; ramp agora cobre apenas aceleracao alem do land drop normal.
- Cartas dupla face/MDFC passam a usar nome, custo, tipo, texto e imagem da face principal quando resolvidas pelo Scryfall.
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
- Endpoint `POST /meta/sync` e resumo administrativo aceitam JWT Google allowlistado por e-mail, mantendo `X-Admin-Key` para Swagger e operacao tecnica.
- Formulario de contato usa endpoint externo configuravel sem persistir mensagens no banco da aplicacao, sem anexos e sem envio automatico de tokens ou dados da sessao.
- Fluxo Google documentado para escopos minimos `openid`, `email` e `profile`, sem persistencia de `access_token` ou `refresh_token`.
- Logs de fluxos autenticados deixam de registrar payloads de decks, nomes de cartas em validacoes e detalhes de troca de recomendacao.
- Diagnostico de seguranca passa a registrar logs operacionais sem tokens, secrets, payloads completos, dados pessoais ou detalhes sensiveis de infraestrutura.
- Sessao no frontend passa a validar issuer/audience/exp do ID token, limpar credenciais em 401 e evitar persistencia legada em `localStorage`.
