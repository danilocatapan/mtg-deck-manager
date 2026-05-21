Objetivo
--------
Versao: agents-2026-05-21
Ultima atualizacao: 2026-05-21

Este e o bootstrap das instrucoes de agentes/Copilot para o MTG Deck Manager. Encaminhe sempre para o arquivo canonico `./.github/agents/AGENTS.md` antes de qualquer acao. Codex tambem possui um ponto de entrada na raiz em `./AGENTS.md`, que aponta para a mesma fonte canonica.

Ordem obrigatoria de leitura
----------------------------
1. `./.github/agents/AGENTS.md` (arquivo canonico, leia sempre).
2. O guia especializado aplicavel:
   - `analysis.md`
   - `backend-quarkus.md`
   - `frontend-react.md`
   - `testing.md`
   - `workflow-graph.md`

Regras de parada obrigatoria
----------------------------
- Se houver duvida sobre comportamento observavel, contratos ou invariantes, pare e consulte `.github/agents/AGENTS.md`.
- E proibido alterar regra negocial, fluxo negocial, criterio de decisao, side effect negocial ou contrato observado pelo usuario sem solicitacao explicita; registre como achado e volte para o arquivo canonico.

Nota curta
----------
Este arquivo e apenas um encaminhador (bootstrap). Nao replique regras globais aqui; elas vivem em `.github/agents/AGENTS.md`.
