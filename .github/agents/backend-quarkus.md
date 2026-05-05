# backend-quarkus.md — Implementação e refatoração segura (Java + Quarkus)

Quando usar
-----------
- Implementar ou refatorar endpoints, serviços, entidades ou fluxo de recomendação no backend Quarkus.

Regras de arquitetura (resumido)
-------------------------------
- Mantém separação clara: Controller (resource) → Service → Repository/Entity.  
- Evite lógica de negócio em controllers; coloque em `service` ou `use-case`.  
- Persistência via Panache/Hibernate; alterações de entidade exigem migração/versionamento.

Fluxo de implementação recomendado
----------------------------------
1. Localize testes relacionados (unit + integration).  
2. Crie ou atualize testes antes de alterar comportamento.  
3. Implemente mudanças em `service` e rode testes isolados.  
4. Atualize recursos REST e documentação (OpenAPI) se houver contrato novo.  

Checklists por tipo de mudança
-----------------------------
- Endpoint/Resource: validar path, parâmetros, códigos HTTP, segurança (roles).  
- Service/Use-case: testar regras de negócio com unit tests; evitar side-effects diretos.  
- Entity/DB: preparar migration/script; atualizar testes de integração.  
- Algoritmo (recommender): isolar scorer, adicionar testes de regressão com decks reais.

Guardrails de refatoração segura
--------------------------------
- Não altere contrato público sem atualização de OpenAPI e testes de contrato.  
- Não remova validações que suportam invariantes (color identity, deck size).  
- Prefira extrair componente (`DeckCompleter`, `MetaProvider`, `SynergyEngine`) em vez de injetar lógica em recursos.

Hotspots do repositório (onde revisar primeiro)
----------------------------------------------
- `RecommendationService` — orquestra pipeline.  
- `CardService` — integra Scryfall (cache e queries).  
- `meta` package — ingestão e dataset local.  
- `model`/`domain` — entidades e DTOs que servem de contrato.

Validação mínima
----------------
- Compilar, rodar testes unitários e executar um teste manual de recomendação (gera 99 cards).  
- Validar logs e métricas mínimos; confirmar que não há regressão de contrato.

Executando Maven (Windows PowerShell)
-----------------------------------
Antes de executar `mvn` no PowerShell, exporte o `JAVA_HOME` e atualize o `Path` com o JDK adequado. Exemplo usado neste repositório:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Em seguida, execute os comandos Maven habituais, por ex.: `./mvnw.cmd clean test`.
