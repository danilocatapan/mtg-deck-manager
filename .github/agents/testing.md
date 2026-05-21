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
- Mudanca de frontend -> lint/build e validacao manual; adicionar testes quando houver harness configurado.

Matriz rapida de decisao
------------------------
- Unit tests: logica pura (scorers, taggers, helpers, normalizers).
- Integration tests: servicos que usam DB, cache ou RestClient.
- Controller/contract: validacao de rota completa e formatos JSON.
- Frontend validation: ESLint, Vite build e fluxo manual focado.

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
- Auth/OIDC, CORS, headers e logs sanitizados quando seguranca for afetada.
- Erros de contrato REST e respostas HTTP.

Exemplos uteis do projeto
-------------------------
- `DeckControllerTest`, `DeckControllerRecommendationTest`, `DeckControllerAnalysisTest`.
- `MetaTopDeckControllerTest`, `MetaExternalDeckImportTest`, `PublicDeckControllerLikeTest`, `UserPrivacyControllerTest`.
- `RecommendationServiceTest`, `StrategicRecommendationServiceTest`, `RecommendationScoringTest`.
- `DeckCompleterTest`, `DeckAnalysisServiceTest`, `ClassificationServiceTest`.
- `MetaDatasetLoaderTest`, `MetaDatasetServiceTest`, `MetaProviderImplTest`, `MetaTopDeckSignalBuilderTest`.
- `SynergyEngineTest`, `CardTaggerTest`.
- `CommanderBracketServiceTest`, `CommanderGameChangerServiceTest`, `DeckLegalityRegressionTest`.

Checklist rapido
----------------
1. Identifique o menor conjunto de testes que a mudanca deve impactar.
2. Garanta que todos os testes relevantes passem localmente.
3. Se a mudanca altera contratos, adicione/atualize testes de controller/contrato.

Comandos atuais
---------------
- Backend: em `backend`, rode `./mvnw.cmd test` no Windows ou `./mvnw test` no Linux/macOS.
- Backend com PostgreSQL local: suba `docker compose up -d postgres` e rode em `backend` com variaveis PostgreSQL/Flyway quando a mudanca tocar persistencia.
- Frontend: em `frontend`, rode `npm run lint` e `npm run build`.
- O frontend ainda nao possui script `test`; nao exigir `npm test` ate ele ser adicionado ao `package.json`.

Executando Maven nos testes (Windows PowerShell)
------------------------------------------------
No Windows, antes de chamar `mvn`/`./mvnw.cmd` no PowerShell, exporte o `JAVA_HOME` apontando para o JDK desejado:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Depois execute: `cd backend` seguido de `./mvnw.cmd test`.
