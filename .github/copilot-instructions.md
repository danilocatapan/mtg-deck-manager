Objetivo
-------
Este é o bootstrap das instruções de agentes/Copilot para este microserviço. Encaminhe sempre para o arquivo canônico `./.github/agents/AGENTS.md` antes de qualquer ação. Use apenas o guia especializado necessário para a tarefa em mãos.

Ordem obrigatória de leitura
---------------------------
1. `./.github/agents/AGENTS.md` (arquivo canônico, leia sempre)
2. O guia especializado aplicável (ex.: `backend-quarkus.md`, `analysis.md`, `testing.md`, `workflow-graph.md`)

Regras de parada obrigatória
---------------------------
- Se houver dúvida sobre comportamento observável, contratos ou invariantes, pare e consulte `AGENTS.md`.
- É proibido alterar regra negocial, fluxo negocial, critério de decisão, side effect negocial ou contrato observado pelo usuário sem solicitação explícita — registre como achado e volte para `AGENTS.md`.

Nota curta
---------
Este arquivo é apenas um encaminhador (bootstrap). Não replique regras globais aqui; elas vivem exclusivamente em `AGENTS.md`.
