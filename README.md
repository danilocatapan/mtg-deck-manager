# MTG Deck Manager

Aplicacao para cadastrar, importar, analisar e otimizar decks Commander.

## LGPD e privacidade

- Autenticacao: Google OIDC via Google Identity Services no frontend. O app deve usar apenas os escopos `openid`, `email` e `profile`.
- Tokens Google: o backend valida o ID token recebido como Bearer token e nao persiste `access_token` nem `refresh_token`. O frontend guarda o ID token apenas em `sessionStorage`, respeitando o `exp` do JWT.
- Dados pessoais tratados: Google subject/id, nome, e-mail, avatar, decks, comandantes, cartas, historico de trocas e auditorias de recomendacao.
- Exportacao: usuario autenticado pode chamar `GET /users/me/export` para baixar seus dados em JSON.
- Exclusao: usuario autenticado pode chamar `DELETE /users/me` para remover decks e auditorias vinculados ao Google subject/id autenticado.
- Cookies/sessao: a API nao usa cookies de sessao para autenticacao; chamadas autenticadas usam `Authorization: Bearer <google-id-token>` e `credentials: omit` no frontend.
- Logs: o nivel padrao da categoria da aplicacao e `WARN`. Nao registre tokens, cookies, header `Authorization`, payloads de decks ou PII. Prefira IDs tecnicos, status, duracao, contagens e codigos de motivo.
- Politica de Privacidade: o frontend publica `privacy-policy.html` com dados coletados, finalidade, base legal provavel, retencao, compartilhamento com Google, direitos do titular e contato.

## Variaveis sensiveis

Nunca commitar valores reais em `.env`, workflows ou documentacao publica.

- `GOOGLE_CLIENT_ID`: client ID OIDC do Google usado pelo backend para validar ID tokens.
- `VITE_GOOGLE_CLIENT_ID`: client ID exposto ao frontend para iniciar o login Google.
- `QUARKUS_DATASOURCE_PASSWORD`: senha do banco.
- `SPICERACK_API_KEY` e `TOPDECK_API_KEY`: chaves de integracoes externas.
- `STAGING_BEARER_TOKEN`: ID token temporario para smoke tests; trate como segredo e rotacione.
- `BACKEND_DEPLOY_HOOK_URL`: webhook de deploy; mantenha apenas em secrets.

## Validacao local

Backend, no Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm run lint
npm run build
```
