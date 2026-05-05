# testing.md — Estratégia prática de testes

Quando usar
-----------
- Para decidir rapidamente que tipo de teste criar/rodar ao alterar código (unit/integration/e2e).

Escolha em 30 segundos
----------------------
- Mudança de regra de negócio ou algoritmo → escreva unit tests + regression test com casos reais.  
- Mudança de integração/DB/schema → escreva integration tests e fixtures.  
- Mudança de contrato REST → escreva tests de contrato e endpoint tests.

Matriz rápida de decisão
-----------------------
- Unit tests: lógica pura (scorers, taggers, helpers).  
- Integration tests: serviços que usam DB, cache, ou RestClient (CardService + Scryfall).  
- End-to-end / contract: validação de rota completa e formatos JSON.

Regras de fixture e ambiente
----------------------------
- Preferir fixtures imutáveis pequenos nos testes (objetos Java ou JSON no resources).  
- Use mocks para serviços externos (Scryfall) em unit tests; reserve integration tests para testar mapeamentos/serialização.

Cobertura que merece atenção explícita
-------------------------------------
- Invariantes de domínio (color identity, deck size).  
- Pipeline de recomendação: scorer + completer + filtro (evitar duplicatas).  
- Meta dataset ingestion (parsing/normalization).

Exemplos úteis do projeto
-------------------------
- Testar scorer com um conjunto de `CardResponseDTO` representativos.  
- Fixture: `meta_dataset.json` como fonte para testes de `MetaDatasetLoader`.

Checklist rápido
---------------
1. Identifique o menor conjunto de testes que o PR deve impactar.  
2. Garanta que todos os testes relevantes passem localmente.  
3. Se o PR altera contratos, adicione/atualize testes de contrato.

Executando Maven nos testes (Windows PowerShell)
---------------------------------------------
No Windows, antes de chamar `mvn`/`./mvnw.cmd` no PowerShell, exporte o `JAVA_HOME` apontando para o JDK desejado. Exemplo do ambiente deste projeto:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Depois execute: `cd backend` seguido de `./mvnw.cmd clean test`.
