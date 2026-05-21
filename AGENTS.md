# AGENTS.md - Instrucoes para Codex

Versao: agents-2026-05-21
Ultima atualizacao: 2026-05-21

Este arquivo e o ponto de entrada do Codex neste repositorio.

## Ordem de leitura

1. Leia `.github/agents/AGENTS.md` antes de qualquer alteracao.
2. Leia apenas os guias especializados necessarios em `.github/agents/`:
   - `analysis.md` para bugs, regressao e impacto.
   - `backend-quarkus.md` para backend Java/Quarkus.
   - `frontend-react.md` para frontend React/Vite.
   - `testing.md` para estrategia de testes.
   - `workflow-graph.md` para pipeline de recomendacao.
3. Use `.github/copilot-instructions.md` somente como bootstrap historico para Copilot; a fonte canonica fica em `.github/agents/AGENTS.md`.

## Regras para o Codex

- Trate as instrucoes em `.github/agents` como regras locais do projeto.
- Nao duplique regras globais neste arquivo; atualize os arquivos em `.github/agents` e `PROJECT_CONTEXT.md` quando stack, contratos, dados ou fluxos principais mudarem.
- Antes de alterar comportamento observavel, valide contratos REST, testes existentes e invariantes de dominio.
- Nao altere regra negocial, fluxo de recomendacao, criterio de score, side effects ou contratos publicos sem pedido explicito.
- Preserve a separacao atual entre backend (`backend/`) e frontend (`frontend/`).
- Para economizar tokens, leia primeiro o arquivo canonico e depois apenas os guias/arquivos diretamente ligados ao pedido; use `rg` para localizar codigo antes de abrir arquivos grandes.

## Validacao padrao

- Backend: em `backend`, rode `./mvnw.cmd test` no Windows ou `./mvnw test` no Linux/macOS.
- Frontend: em `frontend`, rode `npm run lint` e `npm run build`.
- Quando a mudanca afetar recomendacoes, valide tambem um fluxo manual ou teste representativo que cubra color identity, duplicatas e tamanho do deck.
