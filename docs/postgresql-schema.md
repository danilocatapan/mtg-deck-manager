# PostgreSQL Schema Mapping

Versao docs: 2026-05-26
Ultima atualizacao: 2026-05-26

## Objetivo

Este documento resume o schema PostgreSQL atual do MTG Deck Manager e como ele se relaciona com as entidades Java. A fonte executavel do schema sao as migrations Flyway em `backend/src/main/resources/db/migration`.

## Runtime

- `%dev` e `%test`: H2 em memoria com schema gerado para velocidade de desenvolvimento/testes.
- `%pg`: PostgreSQL local production-like com Flyway automatico.
- `%prod`: PostgreSQL com Hibernate em `validate`; Flyway so roda quando `QUARKUS_FLYWAY_MIGRATE_AT_START=true`.

## Migrations Atuais

- `V1__create_deck_schema.sql`: tabelas base de decks e cartas.
- `V2__add_commander_contract_fields.sql`: campos de contrato Commander.
- `V3__add_deck_card_zones.sql`: zonas de carta.
- `V4__add_deck_history_and_zone_unique_index.sql`: historico e indice unico por zona.
- `V5__remove_deck_card_zones.sql`: remocao do modelo persistido de zonas.
- `V6__create_recommendation_audit_runs.sql`: auditoria de recomendacoes.
- `V7__add_deck_visibility_and_author.sql`: visibilidade e autor publico.
- `V8__add_external_decks_and_likes.sql`: decks externos/publicos e likes.
- `V9__create_meta_top_decks.sql`: top decks de meta e lotes de importacao.
- `V10__add_card_printing_metadata.sql`: metadados de impressao de cartas.
- `V11__create_meta_combos.sql`: cache local de combos e cartas de combo.

## Entidades Persistidas

- `Deck`: cabecalho do deck, dono, comandante, identidade de cor, visibilidade, autor publico, origem externa e relacionamento com cartas.
- `DeckCard`: linhas de cartas do deck e quantidades.
- `DeckLike`: voto unico por usuario em deck publico.
- `RecommendationAuditRun`: auditoria de recomendacoes geradas, trocas aplicadas/desfeitas e feedback.
- `MetaTopDeck`: snapshot persistido de top deck externo/rankeado.
- `MetaTopDeckCard`: cartas do top deck por secao/quantidade.
- `MetaTopDeckImportBatch`: lote/status de importacao de top decks.
- `MetaCombo`: definicao persistida de combo conhecida por fonte externa/local.
- `MetaComboCard`: cartas normalizadas que compoem cada combo, incluindo slot de comandante quando aplicavel.

Registros auxiliares em `service/meta` como `MetaDeck`, `MetaCard`, `CommanderMetaProfile` e similares podem ser dados transitorios ou derivados; so viram tabela quando houver entidade JPA e migration correspondente.

## Indices e Invariantes Importantes

- Isolamento por usuario depende de `owner_id`.
- Decks publicos dependem de `visibility` e DTOs publicos sanitizados.
- Likes devem permanecer unicos por deck/usuario.
- Nomes de cartas por deck devem evitar duplicidade indevida; multiplas copias legitimas continuam representadas por `quantity`.
- Auditorias devem guardar rastreabilidade suficiente sem exigir tokens, e-mail ou payload sensivel.
- Top decks devem preservar origem, periodo, formato, bracket/arquetipo quando disponivel, rank e cartas para recalculo de sinais de meta.
- Combos persistidos devem manter unicidade por `source` + `external_id` e indice por nome normalizado de carta para deteccao rapida.

## Regras para Alterar Schema

1. Nunca depender de `drop-and-create` para producao.
2. Toda mudanca de entidade persistida precisa de nova migration Flyway.
3. Mantenha migrations idempotentes apenas quando fizer sentido operacional; migrations versionadas devem ser deterministicas.
4. Avalie indices quando adicionar consulta por owner, visibilidade, comandante, periodo, fonte ou rank.
5. Rode testes H2 e PostgreSQL quando schema, repository ou query mudar.
6. Atualize `PROJECT_CONTEXT.md`, este arquivo e `CHANGELOG.md` quando houver mudanca relevante de persistencia.

## Validacao PostgreSQL Local

```powershell
docker compose up -d postgres
cd backend
$env:QUARKUS_DATASOURCE_DB_KIND = "postgresql"
$env:QUARKUS_DATASOURCE_JDBC_URL = "jdbc:postgresql://localhost:5432/mtg_deck_manager"
$env:QUARKUS_DATASOURCE_USERNAME = "mtg"
$env:QUARKUS_DATASOURCE_PASSWORD = "mtg_dev_password"
$env:QUARKUS_FLYWAY_MIGRATE_AT_START = "true"
$env:QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY = "validate"
.\mvnw.cmd test
```
