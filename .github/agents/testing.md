# testing.md - Estrategia pratica de testes

Versao: agents-2026-05-21
Ultima atualizacao: 2026-05-21

Quando usar
-----------
- Para decidir rapidamente que tipo de teste criar/rodar ao alterar codigo (unit, integration, controller/contract, build/lint).

Escolha em 30 segundos
----------------------
- Mudanca de regra de negocio ou algoritmo -> unit tests + regression test com casos representativos.
- Mudanca de integracao/DB/schema -> integration tests e fixtures.
- Mudanca de contrato REST -> tests de controller/contrato e validacao dos DTOs/status HTTP.
- Mudanca de frontend -> lint/build, Playwright/axe quando afetar UI, e validacao manual focada nos fluxos tocados.

Matriz rapida de decisao
------------------------
- Unit tests: logica pura (scorers, taggers, helpers, normalizers).
- Integration tests: servicos que usam DB, cache ou RestClient.
- Controller/contract: validacao de rota completa e formatos JSON.
- Frontend validation: ESLint, Vite build, Playwright e axe para fluxos principais.

Regras de fixture e ambiente
----------------------------
- Preferir fixtures imutaveis pequenos nos testes (objetos Java ou JSON em resources).
- Use mocks para servicos externos (Scryfall e fontes externas de meta) em unit tests.
- Reserve integration tests para mapeamentos, serializacao, persistencia e contratos completos.

Cobertura que merece atencao explicita
--------------------------------------
- Invariantes de dominio (color identity, deck size, duplicatas, comandante).
- Pipeline de recomendacao: scorer + completer + selectors + filtro.
- Meta dataset ingestion, top decks e adapters externos (parsing/normalization, amostra minima, fonte/bracket).
- Importacao de deck e normalizacao de decklist.
- Decks publicos, likes, copia, privacidade e LGPD.
- Persistencia PostgreSQL/Flyway quando entidade, indice, constraint ou query mudar.
- A CI PostgreSQL deve manter o cenário legado V1-V12 -> dados preparados -> V13/V14 antes da suíte completa; não substitua essa validação por busca textual nas migrations.
- Benchmark: 20 fixtures offline, formulas das metricas, concorrencia, preservacao da ultima rodada, anonimato A/B, voto unico e quorum de 3.
- Auth/OIDC, CORS, headers e logs sanitizados quando seguranca for afetada.
- Erros de contrato REST e respostas HTTP.
- Acessibilidade frontend: WCAG 2.2 AA + AAA oportunista, teclado, foco, contraste, landmarks, dialogs, tabs, popovers, live regions e estados vazio/loading/erro.

Exemplos uteis do projeto
-------------------------
- `DeckControllerTest`, `DeckControllerRecommendationTest`, `DeckControllerAnalysisTest`.
- `MetaControllerSecurityTest`, `MetaExternalDeckImportTest`, `PublicDeckControllerLikeTest`, `UserPrivacyControllerTest`.
- `RecommendationServiceTest`, `StrategicRecommendationServiceTest`, `RecommendationScoringTest`.
- `DeckCompleterTest`, `DeckAnalysisServiceTest`, `ClassificationServiceTest`.
- `MetaDatasetLoaderTest`, `MetaDatasetServiceTest`, `ExternalMetaIngestionJobTest`, `MetaProviderImplTest`, `TopDeckMetaAdapterTest`.
- `SynergyEngineTest`, `CardTaggerTest`.
- `CommanderBracketServiceTest`, `CommanderGameChangerServiceTest`, `DeckLegalityRegressionTest`.

Checklist rapido
----------------
1. Identifique o menor conjunto de testes que a mudanca deve impactar.
2. Garanta que todos os testes relevantes passem localmente.
3. Se a mudanca altera contratos, adicione/atualize testes de controller/contrato.

Comandos atuais
---------------
- Backend: em `backend`, no Windows/PowerShell rode Maven sempre com o preambulo do JDK 25 no mesmo comando; no Linux/macOS rode `./mvnw test`.
- Backend com PostgreSQL local: suba `docker compose up -d postgres` e rode em `backend` com variaveis PostgreSQL/Flyway quando a mudanca tocar persistencia.
- Frontend: em `frontend`, rode `npm run lint` e `npm run build`.
- Frontend e2e: em `frontend`, rode `npm run test:e2e` para fluxos anonimos/autenticados com mocks.
- Frontend a11y: em `frontend`, rode `npm run test:a11y` para axe em desktop/mobile e checagem de contraste semantico.
- Playwright e obrigatorio para os fluxos de sucesso do benchmark, revisao cega, feedback agregado e diagnostico sanitizado.
- O fluxo Playwright do Meta Admin anexa screenshots dos marcos de sucesso em desktop e mobile; use essas evidencias junto aos asserts, nunca como substituto deles.
- CI frontend executa lint, build, e2e e a11y; se Playwright falhar, priorize corrigir o comportamento ou o mock em vez de remover cobertura.
- Os testes Playwright autenticados devem simular `sessionStorage` com token fake e interceptar REST pelos contratos existentes; nao usar login Google real.
- Violacoes axe so podem ser aceitas com comentario explicito no teste explicando o motivo, o escopo e o plano de remocao.

Executando Maven nos testes (Windows PowerShell)
------------------------------------------------
No Windows, chame `mvn`/`./mvnw.cmd` no mesmo bloco/comando que exporta `JAVA_HOME` apontando para o JDK 25. Nao rode `./mvnw.cmd test` cru no PowerShell, porque o ambiente pode usar Java 17 e falhar com `class file version 69.0`:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
./mvnw.cmd test
```

Para teste filtrado:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
./mvnw.cmd "-Dtest=NomeDoTeste" test
```
