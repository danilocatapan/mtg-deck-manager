# Production Deploy Runbook

Versao docs: 2026-06-03
Ultima atualizacao: 2026-06-03

## Database

Use PostgreSQL for every non-local runtime. The backend keeps H2 only for `%dev` and `%test`.

Required runtime variables:

```text
QUARKUS_DATASOURCE_DB_KIND=postgresql
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://<host>:5432/<database>
QUARKUS_DATASOURCE_USERNAME=<database-user>
QUARKUS_DATASOURCE_PASSWORD=<database-password>
QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY=validate
```

Flyway migrations live in:

```text
backend/src/main/resources/db/migration
```

Current migrations cover deck schema, Commander contract fields, recommendation audit, deck visibility/public authorship, external/public deck likes, and the canonical TopDeck.gg meta snapshot.

For the first staging migration, enable:

```text
QUARKUS_FLYWAY_MIGRATE_AT_START=true
```

For production, take a PostgreSQL backup or provider snapshot before the first migration. After the first deploy is validated, keep `QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY=validate`; only set `QUARKUS_FLYWAY_MIGRATE_AT_START=true` when a migration rollout is intended.

## Render Environment

In the Render backend service, configure these environment variables:

```text
GOOGLE_CLIENT_ID=<google-oauth-client-id>
CORS_ORIGINS=https://danilocatapan.github.io,http://localhost:5173
FRONTEND_URL=https://danilocatapan.github.io/mtg-deck-manager/
SWAGGER_UI_ENABLED=false
APP_LOG_LEVEL=INFO
META_ADMIN_EMAILS=<comma-separated-admin-emails>
SECURITY_ADMIN_SUBJECTS=<comma-separated-admin-google-subjects>
```

If the service is linked to a Render PostgreSQL database and receives `DATABASE_URL`, the container converts it to the Quarkus JDBC settings automatically.

Render PostgreSQL URLs often appear as `postgresql://user:password@host:5432/database`. Quarkus JDBC needs the JDBC form, so use:

```text
jdbc:postgresql://host:5432/database
```

Keep username and password in `QUARKUS_DATASOURCE_USERNAME` and `QUARKUS_DATASOURCE_PASSWORD`. If you use an external Render database URL, add `?sslmode=require` to the JDBC URL.

When `DATABASE_URL` is not injected by Render, configure the explicit Quarkus variables instead:

```text
QUARKUS_DATASOURCE_DB_KIND=postgresql
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://<render-postgres-host>:5432/<database>
QUARKUS_DATASOURCE_USERNAME=<render-postgres-user>
QUARKUS_DATASOURCE_PASSWORD=<render-postgres-password>
QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY=validate
QUARKUS_FLYWAY_MIGRATE_AT_START=true
```

## Neon PostgreSQL Migration

Use Neon Free only while the database stays comfortably below the free storage limit. Keep a logical backup before switching production because free tiers should not be treated as operational backup.

The local migration helper uses Docker and does not store credentials in the repository:

```powershell
.\tools\migrate-render-postgres-to-neon.ps1
```

By default the helper uses `postgres:18-alpine` so `pg_dump` can connect to Render databases running PostgreSQL 18. Override it only when the source server is newer:

```powershell
$env:POSTGRES_DOCKER_IMAGE = "postgres:<major>-alpine"
```

Prefer the Render External Database URL from the database dashboard. The internal `DATABASE_URL` injected into the Render service may have a host like `dpg-...-a` and is usually reachable only from Render private networking.

After restoring into Neon, configure the Render backend with explicit Quarkus variables so the old `DATABASE_URL` is ignored:

```text
QUARKUS_DATASOURCE_DB_KIND=postgresql
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://<neon-host>:5432/<neon-database>?sslmode=require
QUARKUS_DATASOURCE_USERNAME=<neon-user>
QUARKUS_DATASOURCE_PASSWORD=<neon-password>
QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY=validate
QUARKUS_FLYWAY_MIGRATE_AT_START=false
QUARKUS_DATASOURCE_JDBC_MAX_SIZE=5
```

Keep the previous Render PostgreSQL database available until `GET /app/info`, `GET /meta/sources`, public deck listing and the authenticated deck flows are validated against Neon.

## Backend Runtime Variables

Configure these in the backend hosting provider, not only in GitHub Actions:

```text
GOOGLE_CLIENT_ID=<google-oauth-client-id>
CORS_ORIGINS=https://danilocatapan.github.io,http://localhost:5173
FRONTEND_URL=https://danilocatapan.github.io/mtg-deck-manager/
SWAGGER_UI_ENABLED=false
APP_LOG_LEVEL=INFO
QUARKUS_FLYWAY_MIGRATE_AT_START=false
META_SPICERACK_ENABLED=true
SPICERACK_API_KEY=<optional-spicerack-key>
META_TOPDECK_ENABLED=false
TOPDECK_API_KEY=<optional-topdeck-key>
META_ADMIN_EMAILS=<comma-separated-admin-emails>
SECURITY_ADMIN_SUBJECTS=<comma-separated-admin-google-subjects>
```

## GitHub Actions Configuration

Repository variables:

```text
VITE_API_URL=https://<backend-runtime-url>
VITE_API_BASE_URL=https://<backend-runtime-url>
VITE_GOOGLE_CLIENT_ID=<google-oauth-client-id>
VITE_META_ADMIN_EMAILS=<comma-separated-admin-emails>
VITE_CONTACT_FORM_ENDPOINT=https://formspree.io/f/<form-id>
CORS_ORIGINS=https://danilocatapan.github.io,http://localhost:5173
SWAGGER_UI_ENABLED=false
APP_LOG_LEVEL=INFO
GOOGLE_CLIENT_ID=<google-oauth-client-id>
STAGING_API_URL=https://<staging-backend-runtime-url>
```

For the public Contact page, create a Formspree form for `MTG Deck Manager - Contato`, restrict submissions to `danilocatapan.github.io`, keep the `_gotcha` honeypot enabled, and configure `VITE_CONTACT_FORM_ENDPOINT` with the form endpoint.

Repository secrets:

```text
BACKEND_DEPLOY_HOOK_URL=<hosting-provider-deploy-hook>
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://<host>:5432/<database>
QUARKUS_DATASOURCE_USERNAME=<database-user>
QUARKUS_DATASOURCE_PASSWORD=<database-password>
SPICERACK_API_KEY=<optional-spicerack-key>
TOPDECK_API_KEY=<optional-topdeck-key>
STAGING_BEARER_TOKEN=<short-lived-google-id-token-for-smoke-tests>
```

The CI/CD workflow now runs:

- backend tests with the default test profile
- backend tests against a PostgreSQL service container with Flyway migration and Hibernate validate
- frontend build
- backend image publish to GHCR after both backend jobs pass
- backend deploy hook
- optional staging smoke tests on manual workflow runs
- frontend deploy to GitHub Pages

## Staging Smoke Tests

Manual workflow runs execute staging smoke tests when both values are configured:

```text
STAGING_API_URL
STAGING_BEARER_TOKEN
```

The smoke test calls:

- `GET /meta/sources`
- `POST /decks/import`
- `GET /decks/{id}/export`
- `POST /decks/{id}/recommendations/strategic`
- `DELETE /decks/{id}`

Use a staging database for these tests. The smoke deck is deleted at the end of a successful run.

## Meta Admin Checks

When enabling the top decks admin UI, configure the same admin e-mail allowlist in frontend and backend:

```text
VITE_META_ADMIN_EMAILS=<comma-separated-admin-emails>
META_ADMIN_EMAILS=<comma-separated-admin-emails>
```

Validate with an authorized Google account:

- open the frontend `Meta Admin` screen
- list top decks
- import a small JSON payload in staging
- confirm `POST /meta/sync` consulta TopDeck.gg, persiste o snapshot canonico, reconstrói perfis e preserva o snapshot anterior em falhas sem expor secrets
- confirm strategic recommendations mention/use `meta_top_decks` only when the sample is sufficient

## Promotion Checklist

1. Confirm staging deploy completed with Flyway migration enabled.
2. Confirm staging smoke tests passed.
3. Take a production PostgreSQL snapshot or logical backup.
4. Apply the production deploy with the same image tag that passed staging.
5. Confirm application health and core deck flows.
6. Disable migration-at-start again when no migration is being rolled out.
