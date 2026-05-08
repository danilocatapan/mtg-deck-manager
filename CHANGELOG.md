# Changelog

Todas as mudancas notaveis deste projeto serao documentadas aqui.

O formato segue Keep a Changelog e Semantic Versioning. Em CI/CD, este arquivo e atualizado a partir das tags Git e mensagens de commit da release.

## [0.0.0-local] - 2026-05-08

### Added
- Footer discreto com versao do frontend.
- Tela Sobre com detalhes do produto, frontend e API.
- Endpoint publico para metadados da API.

### Changed
- Build local passa a usar metadados seguros quando o pipeline ainda nao gerou uma versao.
- Analise do deck passa a destacar diagnosticos acionaveis em PT-BR, com alertas de curva, ramp, compra e interacao.
- Cards de recomendacao passam a mostrar impacto antes/depois para curva, ramp, compra, interacao, Game Changers e pressao de bracket.
- Telas de analise e recomendacoes foram compactadas para priorizar os 3-5 sinais mais importantes para jogadores de Commander, deixando detalhes secundarios em uma area avancada recolhida.
