#!/bin/sh
set -eu

if [ -z "${QUARKUS_DATASOURCE_JDBC_URL:-}" ] && [ -n "${DATABASE_URL:-}" ]; then
  database_url="${DATABASE_URL#postgresql://}"
  database_url="${database_url#postgres://}"

  if [ "$database_url" != "${database_url#*@}" ]; then
    credentials="${database_url%%@*}"
    database_url="${database_url#*@}"
    export QUARKUS_DATASOURCE_USERNAME="${QUARKUS_DATASOURCE_USERNAME:-${credentials%%:*}}"
    export QUARKUS_DATASOURCE_PASSWORD="${QUARKUS_DATASOURCE_PASSWORD:-${credentials#*:}}"
  fi

  export QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://${database_url}"
fi

export QUARKUS_DATASOURCE_DB_KIND="${QUARKUS_DATASOURCE_DB_KIND:-postgresql}"
export QUARKUS_FLYWAY_MIGRATE_AT_START="${QUARKUS_FLYWAY_MIGRATE_AT_START:-true}"
export QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY="${QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY:-validate}"

exec /opt/jboss/container/java/run/run-java.sh
