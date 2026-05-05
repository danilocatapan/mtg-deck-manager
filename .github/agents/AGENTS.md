# AGENTS.md — Regras canônicas de agentes / Copilot

Papel deste arquivo
-------------------
Este é o único arquivo canônico com regras globais, prioridades, topologia documental e políticas que orientam todos os agentes/copilot-instructions do repositório.

Decisão de topologia documental
------------------------------
Adotamos explicitamente: 1 bootstrap (`.github/copilot-instructions.md`), 1 arquivo canônico (`AGENTS.md`) e 4 guias especializados (`backend-quarkus.md`, `analysis.md`, `testing.md`, `workflow-graph.md`). Não serão criados atalhos ou resumos duplicados.

Fluxo mínimo obrigatório
------------------------
1. Leia `AGENTS.md` (regra obrigatória). 2. Escolha o guia especializado relevante. 3. Antes de alterar código, execute a análise mínima definida em `analysis.md`.

Regras globais inegociáveis
---------------------------
- Sempre validar contratos observáveis (endpoints + testes) antes de alterar comportamento.
- Não alterar regra negocial, fluxo negocial, critério de decisão, side effects ou contratos observáveis sem pedido explícito: registre como achado e consulte stakeholders.

Congelamento de regra negocial
-----------------------------
Qualquer mudança que possa impactar o comportamento esperado pelo usuário (por ex., remoção de validação, alteração de cálculo, mudança de contrato REST) deve ser tratada como alteração negocial e precisa de aprovação explícita.

Snapshot do projeto (contexto rápido)
------------------------------------
- Stack principal: Java 21+ / Quarkus (backend).  
- Frontend: Vite + React (separado).  
- Persistência: Hibernate ORM / Panache (H2 para dev).  
- Integrações: Scryfall (REST), potencial scraping/EDHREC dataset local.  

Hotspots obrigatórios do domínio
-------------------------------
- Recommendation engine (pipeline: meta → candidates → score → completer).  
- Meta dataset ingestion (EDHREC/Moxfield).  
- Color identity / Commander rules (negócio crítico).  

Invariantes globais do domínio
------------------------------
- Decks Commander devem respeitar color identity; nada fora das cores.  
- Recomendação deve fornecer 99 cartas + comandante = 100.  
- Não introduzir cartas que já existem no deck como sugestão de add.

Prioridades operacionais transversais
------------------------------------
P0: Corrigir violação de invariantes (cores, tamanho, contratos REST).  
P1: Melhorar qualidade de recomendação (meta grounding, sinergia).  
P2: Refatoração de infraestrutura (extrair pipeline, performance).

Validação mínima antes de concluir mudança
-----------------------------------------
- Executar testes automatizados relacionados.  
- Rodar análise estática (format, compile).  
- Validar um caso manual representativo (ex.: gerar recommendations para deck real).  
- Se mudança afetar contrato, adicionar testes de contrato/end-to-end.

Guia rápido de roteamento
------------------------
- Bug/Regressão funcional → `analysis.md` (siga checklist).  
- Mudar implementação de endpoint / service → `backend-quarkus.md` + `testing.md`.  
- Ajuste de algoritmo de recomendação → `backend-quarkus.md` (scorer) + `workflow-graph.md` (pipeline).  
