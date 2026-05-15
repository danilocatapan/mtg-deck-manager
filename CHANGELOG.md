# Changelog

Todas as mudancas notaveis deste projeto serao documentadas aqui.

O formato segue Keep a Changelog e Semantic Versioning. Em CI/CD, este arquivo e atualizado a partir das tags Git e mensagens de commit da release.

## [0.0.0-local] - 2026-05-08

### Added
- Footer discreto com versao do frontend.
- Tela Sobre com detalhes do produto, frontend e API.
- Endpoint publico para metadados da API.

### Changed
- Cards de recomendação agora exibem preview hoverável da imagem das cartas sugeridas para adicionar e remover, usando a resolução existente de imagens de cartas.
- Fluxos do frontend receberam polimento de UX: idioma `pt-BR`, microcopy com acentuação correta, estados de login mais claros, foco de teclado visível e alvos de toque maiores em botões/links.
- Header, stepper e ações principais foram ajustados para reduzir corte e excesso de altura no mobile, mantendo CTAs e mensagens de autenticação mais previsíveis.
- Recomendacoes estrategicas agora usam diagnostico estrutural generico por arquetipo/bracket para priorizar curva, ramp, compra, interacao, protecao, inevitabilidade e combos sem depender de regras por comandante.
- Recomendacoes estrategicas agora persistem snapshots de auditoria com contexto, score breakdown, cut breakdown, pares bloqueados, cortes protegidos e endpoint inicial para feedback humano.
- Recomendacoes estrategicas agora protegem finishers, pecas de combo e payoffs sinergicos contra trocas incoerentes por ramp generico, com logs de auditoria para adds, cuts, bloqueios e pareamentos.
- Fallbacks de ramp removem rocks fora de cor funcional e passam a rejeitar mana que produza cores fora da identidade do comandante.
- Tela de recomendacoes remove a selecao de intencao enquanto o backend estabiliza uma estrategia unica e mais confiavel.
- Pipeline estrategico passa a combinar perfil meta local, staples por arquetipo, upgrades funcionais e pecas que completam combos ja quase presentes.
- Benchmark de Xenagos high-power passa a validar qualidade real das trocas, incluindo adds/cuts esperados contra uma referencia offline, e os dados locais incluem tambem bases para Grand Arbiter e Kess.
- Cadastro, edicao, importacao, analise e recomendacoes agora tratam cartas somente como cartas do deck, sem selecao de grupo.
- Contrato de cartas do deck deixou de expor/enviar campo de grupo, e swaps de recomendacao aplicam remove/add diretamente no deck.
- Build local passa a usar metadados seguros quando o pipeline ainda nao gerou uma versao.
- Analise do deck passa a destacar diagnosticos acionaveis em PT-BR, com alertas de curva, ramp, compra e interacao.
- Cards de recomendacao passam a mostrar impacto antes/depois para curva, ramp, compra, interacao, Game Changers e pressao de bracket.
- Recomendacoes estrategicas passam a aceitar aliases simples de objetivo Commander (`bracket 1` a `bracket 4`) e melhoram fallback high-power/cEDH com sugestoes mais eficientes e diretas.
- Motor estrategico passa a usar classificador simples de roles/arquetipos, sinais de combos locais e adapter TopDeck offline para perfis competitivos.
- Telas de analise e recomendacoes foram compactadas para priorizar os 3-5 sinais mais importantes para jogadores de Commander, deixando detalhes secundarios em uma area avancada recolhida.
- Painel de legalidade Commander recebeu mais respiro entre titulo, status e diagnosticos para melhorar a leitura dos checks.
- Visualizacao por imagens da lista do deck evita sobreposicao dos controles e permite recarregar artes quando a busca em lote falhar temporariamente.
- Tela de edicao do deck foi compactada para trazer a lista de cartas mais cedo, recolher a legalidade quando estiver sem bloqueios e evitar overflow do cabecalho no mobile.
- Visualizacao por lista da edicao do deck agora usa rolagem interna para decks longos, mantendo o proximo passo acessivel apos as cartas.
- Card do comandante na edicao passa a exibir a arte resolvida da carta e remove o texto de legalidade ambigua do resumo.

### Fixed
- Auditorias de recomendacao passam a mapear JSONs persistidos como `TEXT` comum no Hibernate, evitando erro de Large Object nos testes PostgreSQL do CI.
