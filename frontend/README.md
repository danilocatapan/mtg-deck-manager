# Frontend - MTG Deck Manager

Versao docs: 2026-05-21
Ultima atualizacao: 2026-05-21

SPA em React 19 + Vite 8 para criar, importar, consultar, analisar e melhorar decks Commander. O app e publicado com base path `/mtg-deck-manager/`.

## Stack

- React 19.2.
- Vite 8.
- ESLint 10.
- Estado local React, sem router dedicado.
- Google Identity Services no navegador.

## Estrutura

- `src/pages`: telas principais.
- `src/components`: componentes de composicao.
- `src/components/recommendations`: cards/painel/configuracoes de recomendacao.
- `src/components/ui`: primitivos reutilizaveis.
- `src/services/api.js`: cliente REST central.
- `src/services/auth.js`: sessao Google em `sessionStorage`.
- `src/styles/global.css` e `src/index.css`: estilos globais.
- `public`: release notes, politica de privacidade e assets publicos.

## Desenvolvimento

```powershell
npm install
npm run dev
```

Abra:

```text
http://localhost:5173/mtg-deck-manager/
```

Validacao:

```powershell
npm run lint
npm run build
```

O projeto ainda nao possui harness de teste frontend; nao assumir `npm test`.

## Variaveis

- `VITE_API_URL` ou `VITE_API_BASE_URL`: origem da API backend.
- `VITE_GOOGLE_CLIENT_ID`: client ID Google.
- `VITE_META_ADMIN_EMAILS`: allowlist de e-mails para UI admin de meta.
- `VITE_CONTACT_FORM_ENDPOINT`: endpoint publico do formulario de contato.

## Cuidados

- Chamadas REST devem passar por `src/services/api.js`.
- Manter `credentials: omit` e Bearer token vindo de `src/services/auth.js`.
- Token Google e perfil publico devem ficar apenas em `sessionStorage`.
- Validar mobile e desktop quando tocar layout principal.
- Nao mover regra negocial do backend para o frontend.
