# AGENTS.md - Instrucoes para Codex

Versao: agents-2026-05-22
Ultima atualizacao: 2026-05-22

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
4. Para pedidos nao triviais no Codex, consulte `docs/codex-skills.md` e use skills instaladas quando elas melhorarem o resultado.

## Regras para o Codex

- Trate as instrucoes em `.github/agents` como regras locais do projeto.
- Nao duplique regras globais neste arquivo; atualize os arquivos em `.github/agents` e `PROJECT_CONTEXT.md` quando stack, contratos, dados ou fluxos principais mudarem.
- Antes de alterar comportamento observavel, valide contratos REST, testes existentes e invariantes de dominio.
- Nao altere regra negocial, fluxo de recomendacao, criterio de score, side effects ou contratos publicos sem pedido explicito.
- Preserve a separacao atual entre backend (`backend/`) e frontend (`frontend/`).
- Para economizar tokens, leia primeiro o arquivo canonico e depois apenas os guias/arquivos diretamente ligados ao pedido; use `rg` para localizar codigo antes de abrir arquivos grandes.
- O usuario nao precisa citar skills por nome. O Codex deve mapear pedidos amplos para as skills adequadas usando `docs/codex-skills.md`.

## Validacao padrao

- Backend: em `backend`, no Windows/PowerShell sempre rode Maven no mesmo comando que define o JDK 25:
  `$env:JAVA_HOME="C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"; $env:Path="$env:JAVA_HOME\bin;$env:Path"; ./mvnw.cmd test`.
  No Linux/macOS, rode `./mvnw test`.
- Frontend: em `frontend`, rode `npm run lint` e `npm run build`.
- Quando a mudanca afetar recomendacoes, valide tambem um fluxo manual ou teste representativo que cubra color identity, duplicatas e tamanho do deck.
