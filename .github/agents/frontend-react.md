# frontend-react.md - Implementacao e manutencao segura (Vite + React)

Versao: agents-2026-05-21
Ultima atualizacao: 2026-05-21

Quando usar
-----------
- Ao implementar ou refatorar componentes, paginas, servicos de API, estilos ou assets no frontend (Vite + React).

Regras de arquitetura (resumido)
--------------------------------
- Separacao de responsabilidades: Pages -> Components -> Services (API) -> Styles.
- Componentes devem ser pequenos, com props explicitas; logica pesada vai para hooks ou services.
- Comunicacoes com backend devem passar por `src/services/api.js` sempre que representarem chamadas REST.
- Autenticacao Google fica em `src/services/auth.js`; ID token e perfil publico vivem apenas em `sessionStorage`.
- O Vite usa base path `/mtg-deck-manager/`; validar URLs locais com esse path.
- O app atual usa estado local em React, sem router dedicado nesta fase.

Fluxo de implementacao recomendado
----------------------------------
1. Localize componentes, paginas e servicos envolvidos.
2. Verifique o contrato JSON esperado no backend antes de mudar payloads.
3. Faca mudancas isoladas em componentes, services e estilos.
4. Valide com lint, build e um fluxo manual representativo.

Checklists por tipo de mudanca
------------------------------
- UI/Component: acessibilidade, props explicitas, estados vazio/loading/erro e responsividade.
- Page/Flow: preservar navegacao atual por estado local e validar transicoes principais.
- API/Service: centralizar chamadas em `src/services/api.js`, tratar erros e preservar contratos REST.
- Styles: usar variaveis globais; evitar regras CSS globais que quebrem outras paginas.
- Assets: confirmar import/path e build final.

Guardrails de refatoracao segura
--------------------------------
- Nao alterar o contrato REST esperado pelo backend sem coordenacao e teste.
- Nao mover logica de negocio do backend para o frontend.
- Preservar observaveis UX (mensagens de erro, estados tratados, exibicao de resultados) salvo pedido explicito.

Hotspots do repositorio (onde revisar primeiro)
-----------------------------------------------
- `src/services/api.js` - integracao com backend e formatacao de payloads.
- `src/services/auth.js`, `src/components/AuthStatus.jsx` - login Google, sessao, perfil publico e admin meta.
- `src/pages/Home.jsx`, `src/pages/DeckConsultPage.jsx`, `src/pages/MetaTopDeckAdminPage.jsx` - vitrine publica, consulta, copia/like e admin de top decks.
- `src/pages/DeckEditorPage.jsx`, `src/pages/ImportDeckPage.jsx`, `src/components/RecommendationForm.jsx` - fluxo de importacao, edicao e recomendacao.
- `src/components/Recommendations.jsx`, `src/components/DeckAnalysis.jsx` - renderizacao dos resultados do backend.
- `src/components/recommendations/*` - cards, badges, painel e configuracoes de recomendacao.
- `src/components/layout/AppLayout.jsx`, `src/styles/global.css`, `src/index.css` - responsividade, tema e layout.
- `src/pages/ContactPage.jsx`, `src/pages/ReleaseNotesPage.jsx`, `src/services/contactApi.js`, `src/services/releaseNotesApi.js` - paginas publicas e conteudo empacotado.
- Assets em `src/assets` e `public/icons.svg` impactam identidade visual e build.

Validacao minima
----------------
- O projeto ainda nao define script de teste frontend; nao assumir `npm test`.
- Rodar `npm run lint` e corrigir falhas.
- Rodar `npm run build` e verificar warnings relevantes.
- Executar um caso manual quando a mudanca afetar fluxo principal: criar/editar deck, importar lista, analisar deck ou chamar recommendations.

Notas operacionais rapidas
--------------------------
- Centralize chamadas externas e feature flags em `src/services`.
- Preserve `credentials: omit`, `Authorization: Bearer <id-token>` e limpeza de sessao em 401.
- Prefira testes automaticos quando um harness frontend for adicionado; ate la, combine lint/build com validacao manual focada.
- Em duvidas contratuais, volte para `AGENTS.md` e siga a regra: nao alterar regra negocial sem solicitacao explicita.
