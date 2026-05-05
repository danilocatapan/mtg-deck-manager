# frontend-react.md — Implementação e manutenção segura (Vite + React)

Quando usar
-----------
- Ao implementar ou refatorar componentes, páginas, serviços de API, estilos ou testes no frontend (Vite + React).

Regras de arquitetura (resumido)
-------------------------------
- Separação de responsabilidades: Pages → Components → Services (API) → Styles.  
- Componentes devem ser pequenos, com props explícitas; lógica pesada vai para hooks ou services.  
- Comunicações com backend devem passar por `src/services/api.js` (único lugar para chamadas REST).  

Fluxo de implementação recomendado
----------------------------------
1. Localize testes e exemplos de uso (component tests, storybook se existir).  
2. Adicione/atualize unit tests (Vitest/Jest) e component/integration tests.  
3. Faça mudanças isoladas em componentes e hooks; valide via story/test.  
4. Atualize chamadas a `services/api.js` e valide contratos JSON com o backend.

Checklists por tipo de mudança
-----------------------------
- UI/Component: accessibility (a11y), props API, isoladamente testável.  
- Page/Route: validação de params, lazy-loading, code-splitting.  
- API/Service: centralizar em `src/services/api.js`, tratar erros e retries.  
- Styles: usar variáveis globais; evitar regras CSS globais que quebrem outras páginas.

Guardrails de refatoração segura
--------------------------------
- Não alterar o contrato REST esperado pelo backend sem coordenação; teste com dados reais.  
- Não mover lógica de negócio do backend para o frontend (cálculos de regras negocial).  
- Preservar observáveis UX (mensagens de erro, códigos HTTP tratados) — registre como achado se precisar mudar.

Hotspots do repositório (onde revisar primeiro)
----------------------------------------------
- `src/services/api.js` — integração com backend e formatação de payloads.  
- `src/pages/DeckEditorPage.jsx`, `src/components/RecommendationForm.jsx` — fluxo de recomendação.  
- `src/components/layout` — responsividade e temas.  
- Tests: `src/__tests__` ou arquivos com `.test.jsx` — primeiro ponto para validar mudanças.

Validação mínima
----------------
- Rodar `npm test` (ou `pnpm`/`yarn` conforme projeto) e corrigir falhas.  
- Rodar build local: `npm run build` e verificar bundle size / console warnings.  
- Executar um caso manual: abrir página de edição de deck, chamar recommendations, validar que UI mostra 99 resultados sem erros.

Notas operacionais rápidas
-------------------------
- Centralize chamadas externas e feature flags em `src/services`.  
- Prefira testes automáticos a validação manual para evitar regressões visuais.  
- Em dúvidas contratuais, voltar para `AGENTS.md` e seguir a regra: não alterar regra negocial sem solicitação explícita.
