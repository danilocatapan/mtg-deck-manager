# Production Deploy Runbook

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

## Backend Runtime Variables

Configure these in the backend hosting provider, not only in GitHub Actions:

```text
GOOGLE_CLIENT_ID=<google-oauth-client-id>
CORS_ORIGINS=https://danilocatapan.github.io,http://localhost:5173
FRONTEND_URL=https://danilocatapan.github.io/mtg-deck-manager/
SWAGGER_UI_ENABLED=false
APP_LOG_LEVEL=INFO
QUARKUS_FLYWAY_MIGRATE_AT_START=false
```

## GitHub Actions Configuration

Repository variables:

```text
VITE_API_URL=https://<backend-runtime-url>
VITE_GOOGLE_CLIENT_ID=<google-oauth-client-id>
CORS_ORIGINS=https://danilocatapan.github.io,http://localhost:5173
SWAGGER_UI_ENABLED=false
APP_LOG_LEVEL=INFO
GOOGLE_CLIENT_ID=<google-oauth-client-id>
STAGING_API_URL=https://<staging-backend-runtime-url>
```

Repository secrets:

```text
BACKEND_DEPLOY_HOOK_URL=<hosting-provider-deploy-hook>
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://<host>:5432/<database>
QUARKUS_DATASOURCE_USERNAME=<database-user>
QUARKUS_DATASOURCE_PASSWORD=<database-password>
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

## Promotion Checklist

1. Confirm staging deploy completed with Flyway migration enabled.
2. Confirm staging smoke tests passed.
3. Take a production PostgreSQL snapshot or logical backup.
4. Apply the production deploy with the same image tag that passed staging.
5. Confirm application health and core deck flows.
6. Disable migration-at-start again when no migration is being rolled out.
