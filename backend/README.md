# Backend - MTG Deck Manager API

Versao docs: 2026-05-21
Ultima atualizacao: 2026-05-21

API REST em Java 25 + Quarkus 3.35.2 para decks Commander, cartas, meta, recomendacoes, privacidade e administracao operacional.

## Stack

- Quarkus REST + Jackson.
- Hibernate ORM/Panache.
- H2 em `%dev` e `%test`.
- PostgreSQL/Flyway em `%pg` e `%prod`.
- Google OIDC validando ID token Bearer.
- REST Clients: Scryfall, Spicerack e TopDeck.gg.
- Testes: JUnit, Quarkus Test, Mockito, RestAssured.

## Estrutura

- `controller`: endpoints REST.
- `dto`: contratos JSON.
- `model`/`repository`: entidades e consultas.
- `service`: regra de negocio, decks, analise, recomendacoes, auditoria, privacidade.
- `service/meta`: dataset local, top decks, adapters externos e agregacao.
- `service/rules`: banlist, brackets e game changers Commander.
- `service/synergy`: tags e motor de sinergia.
- `client`: clients HTTP externos.
- `config`: headers, CORS, logs, mappers e contexto de request.

## Desenvolvimento

H2 local:

```powershell
$env:JAVA_HOME = "C:\Users\danilo.catapan\Documents\Java\jdk-25.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd quarkus:dev
```

PostgreSQL local:

```powershell
docker compose up -d postgres
.\mvnw.cmd quarkus:dev -Dquarkus.profile=pg
```

Testes:

```powershell
.\mvnw.cmd test
```

Package JVM:

```powershell
.\mvnw.cmd package
```

## Contratos Principais

- `/cards`
- `/decks`
- `/public/decks`
- `/meta`
- `/recommendation-audits`
- `/users/me`
- `/security/status`
- `/app/info`

Consulte `PROJECT_CONTEXT.md` para a lista de endpoints atualizada.

## Regras Criticas

- Nao alterar contrato REST, regra de negocio, score ou fluxo de recomendacao sem teste e pedido explicito.
- Toda entidade persistida nova/alterada precisa de migration Flyway.
- Deck privado nunca aparece em endpoint publico.
- DTO publico nunca expoe `owner_id`, e-mail, avatar, auditoria ou historico de troca.
- Recomendacao nunca sugere carta fora da color identity, duplicata ou corte do comandante.
- Logs nao podem conter tokens, Authorization, payload completo de deck ou PII desnecessaria.
