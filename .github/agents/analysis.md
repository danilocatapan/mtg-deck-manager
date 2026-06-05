# analysis.md - Analise de impacto, bug e regressao

Versao: agents-2026-05-21
Ultima atualizacao: 2026-05-21

Responsabilidade
----------------
Fornecer passos concisos para diagnosticar bugs, avaliar impacto e propor a menor correcao segura. Este arquivo cobre analise de impacto, regressao e diagnostico; nao existe `debug.md` separado.

Quando usar
-----------
- Antes de qualquer mudanca que altere comportamento observavel.
- Ao investigar falhas relatadas em producao, testes automatizados, CI ou fluxos manuais.
- Ao tocar contratos REST, persistencia, recomendacoes, importacao de deck, meta dataset ou integracoes externas.

Passos obrigatorios (ordem)
---------------------------
1. Reproduza localmente quando possivel: identifique entrada (request/acao) e saida atual vs esperada.
2. Rode testes relevantes unit/controller/integration; capture falhas.
3. Identifique o menor cenario reproduzivel.
4. Busque causa raiz (logs, stacktrace, contrato, fixtures, historico de commits se necessario).
5. Liste side effects potenciais (DB, integracoes, cache, UI, contratos, CI/CD).
6. Proponha a menor correcao segura e o plano de validacao.

Trilho adicional para bug e regressao
-------------------------------------
- Para regressao em testes, analise primeiro os testes que dependem do contrato afetado e execute o menor conjunto antes da suite completa.
- Para falhas de CI, compare o comando local equivalente (`./mvnw test`, `npm run lint`, `npm run build`) antes de alterar workflow.
- Para mudancas de recomendacao, valide invariantes: color identity, duplicatas, tamanho alvo e cortes.

Checklist operacional de conformidade arquitetural
--------------------------------------------------
- Verifique contratos REST expostos e testes de controller.
- Verifique mudancas em entidades persistidas e impacto de schema.
- Verifique configuracoes de cache, CORS, auth/OIDC e headers de seguranca quando afetadas.
- Confirme compatibilidade de integracoes externas (Scryfall, TopDeck.gg, Commander Spellbook e adapters ativos).
- Confirme que frontend e backend continuam alinhados em payloads JSON.
- Para dados publicos/LGPD, confirme isolamento por `owner_id`, visibilidade, DTOs sanitizados, exportacao/exclusao e ausencia de PII em logs.
- Para PostgreSQL, confirme migration Flyway, constraints/indices, teste `%pg`/CI quando entidade persistida mudar.

Classificacao de achados (P0/P1/P2)
-----------------------------------
- P0: quebra de invariantes, perda de dados, falha de seguranca ou violacao de contrato.
- P1: degradacao perceptivel de recomendacao, erro repetido ou falha relevante de UX/API.
- P2: refactor, melhoria de performance, limpeza ou adicao de testes sem impacto imediato.

Saida esperada
--------------
- Relatorio curto com cenario reproduzivel, testes falhando ou executados, causa raiz proposta, impacto estimado e plano de validacao.
