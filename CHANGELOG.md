# Changelog

Todas as mudancas notaveis deste projeto serao documentadas aqui.

O formato segue Keep a Changelog e Semantic Versioning. Em CI/CD, este arquivo e atualizado a partir das tags Git e mensagens de commit da release.

## [0.0.0-local] - 2026-05-08

### Added
- Footer discreto com versao do frontend.
- Tela Sobre com detalhes do produto, frontend e API.
- Endpoint publico para metadados da API.

### Changed
- Cadastro, edicao, importacao, analise e recomendacoes agora tratam cartas somente como cartas do deck, sem selecao de grupo.
- Contrato de cartas do deck deixou de expor/enviar campo de grupo, e swaps de recomendacao aplicam remove/add diretamente no deck.
- Build local passa a usar metadados seguros quando o pipeline ainda nao gerou uma versao.
- Analise do deck passa a destacar diagnosticos acionaveis em PT-BR, com alertas de curva, ramp, compra e interacao.
- Cards de recomendacao passam a mostrar impacto antes/depois para curva, ramp, compra, interacao, Game Changers e pressao de bracket.
- Telas de analise e recomendacoes foram compactadas para priorizar os 3-5 sinais mais importantes para jogadores de Commander, deixando detalhes secundarios em uma area avancada recolhida.
- Painel de legalidade Commander recebeu mais respiro entre titulo, status e diagnosticos para melhorar a leitura dos checks.
- Visualizacao por imagens da lista do deck evita sobreposicao dos controles e permite recarregar artes quando a busca em lote falhar temporariamente.
- Tela de edicao do deck foi compactada para trazer a lista de cartas mais cedo, recolher a legalidade quando estiver sem bloqueios e evitar overflow do cabecalho no mobile.
- Visualizacao por lista da edicao do deck agora usa rolagem interna para decks longos, mantendo o proximo passo acessivel apos as cartas.
