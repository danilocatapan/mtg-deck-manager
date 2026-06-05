# backend-quarkus.md - Implementacao e refatoracao segura (Java + Quarkus)

Versao: agents-2026-05-21
Ultima atualizacao: 2026-05-21

Quando usar
-----------
- Implementar ou refatorar endpoints, servicos, entidades, DTOs, repositories, clients ou fluxo de recomendacao no backend Quarkus.

Regras de arquitetura (resumido)
--------------------------------
- Manter separacao clara: Controller -> Service -> Repository/Entity/Client.
- Evitar logica de negocio em controllers; colocar em `service` ou componentes de dominio.
- Persistencia via Panache/Hibernate; alteracoes de entidade exigem migration Flyway em `backend/src/main/resources/db/migration`, avaliacao PostgreSQL e testes.
- Contratos publicos vivem em controllers + DTOs; qualquer mudanca observavel exige teste.
- Google OIDC deve continuar baseado em ID token Bearer; nao persistir access/refresh tokens.
- Logs devem usar dados tecnicos, contagens e codigos; nunca tokens, Authorization, payload completo de deck ou PII desnecessaria.
- Rodadas de benchmark usam recursos versionados offline, persistem versoes/resultados e nao chamam integracoes externas.

Fluxo de implementacao recomendado
----------------------------------
1. Localize testes relacionados (unit + controller/integration).
2. Crie ou atualize testes antes ou junto da mudanca de comportamento.
3. Implemente mudancas em services/componentes e rode testes isolados.
4. Atualize recursos REST e documentacao OpenAPI se houver contrato novo.

Checklists por tipo de mudanca
------------------------------
- Endpoint/Resource: validar path, parametros, status HTTP, erros, autenticacao/autorizacao quando aplicavel.
- Service/Use-case: testar regras de negocio com unit tests; evitar side effects diretos e escondidos.
- Entity/DB: avaliar migration/script, defaults e testes de persistencia.
- Algoritmo/recommender: isolar scorer/selectors/completer, adicionar testes de regressao com decks representativos.
- Integracao externa: preservar isolamento com mocks em testes e tratar rate limit/erros (`ExternalServiceException`, `RateLimitedExternalServiceException`).

Guardrails de refatoracao segura
--------------------------------
- Nao alterar contrato publico sem atualizar testes de controller/contrato.
- Nao remover validacoes que suportam invariantes (color identity, deck size, duplicidade).
- Prefira extrair ou ajustar componentes existentes (`DeckCompleter`, `MetaProvider`, `SynergyEngine`, selectors) em vez de concentrar logica em controllers.

Hotspots do repositorio (onde revisar primeiro)
-----------------------------------------------
- `RecommendationService` e `StrategicRecommendationService` - orquestram recomendacoes heuristicas e estrategicas.
- `RecommendationAuditService`, `RecommendationAuditRepository`, apply/undo swap e feedback - rastreabilidade de trocas recomendadas.
- `RecommendationBenchmarkService` e repositories `RecommendationBenchmark*` - metricas offline, rodadas e revisao cega.
- `DeckCompleter`, `RecommendationScoring`, `RecommendationPairer`, `CandidateAddSelector`, `CandidateCutSelector` - pipeline de completar, pontuar e parear sugestoes.
- `CardService` e `ScryfallClient` - integracao Scryfall, cache e queries.
- `service/meta`, `ExternalMetaIngestionJob`, `MetaDatasetService`, `TopDeckMetaAdapter` - ingestao automatica, persistencia canonica, normalizacao e fallback local.
- `DeckImportService` e `service/meta/DecklistNormalizer` - parsing e normalizacao de listas.
- `PublicDeckController`, `DeckLikeRepository`, `UserPrivacyController` - decks publicos, likes, copia e LGPD.
- `service/rules` - banlist, brackets e game changers Commander.
- `controller` + `dto` - contratos REST observaveis.
- `config` - headers, auth, mappers de erro e configuracao transversal.

Validacao minima
----------------
- Compilar e rodar testes backend relevantes.
- Para mudanca ampla, rodar suite backend completa.
- Executar um teste manual de recomendacao/importacao quando afetar fluxo de deck.
- Confirmar que nao ha regressao de contrato REST.

Executando Maven (Windows PowerShell)
-------------------------------------
Sempre execute `mvn`/`./mvnw.cmd` no mesmo bloco/comando que exporta `JAVA_HOME` e atualiza o `Path` com o JDK adequado. Nao rode `./mvnw.cmd test` cru no PowerShell, porque o ambiente pode usar Java 17 e falhar ao carregar classes compiladas para Java 25:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
./mvnw.cmd test
```

Para testes filtrados, preserve o mesmo preambulo:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
./mvnw.cmd "-Dtest=NomeDoTeste" test
```
