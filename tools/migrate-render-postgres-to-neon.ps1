param(
    [string]$SourceUrl = $env:RENDER_DATABASE_URL,
    [string]$TargetUrl = $env:NEON_DATABASE_URL,
    [string]$PostgresImage = $(if ($env:POSTGRES_DOCKER_IMAGE) { $env:POSTGRES_DOCKER_IMAGE } else { "postgres:18-alpine" }),
    [string]$BackupDir = (Join-Path $env:USERPROFILE "Documents\mtg-db-backups"),
    [string]$BackupFile = "",
    [switch]$SkipDump,
    [switch]$SkipRestore,
    [switch]$ValidateOnly
)

$ErrorActionPreference = "Stop"

function Read-SecretText {
    param([string]$Prompt)

    $secure = Read-Host $Prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function Get-PostgresUrlInfo {
    param([string]$Url)

    $uri = [Uri]$Url
    $db = $uri.AbsolutePath.TrimStart("/")
    $port = if ($uri.Port -gt 0) { $uri.Port } else { 5432 }
    $user = ""

    if ($uri.UserInfo) {
        $user = [Uri]::UnescapeDataString(($uri.UserInfo -split ":", 2)[0])
    }

    [pscustomobject]@{
        Host = $uri.Host
        Port = $port
        Database = $db
        User = $user
        JdbcUrl = "jdbc:postgresql://$($uri.Host):$port/$db$(if ($uri.Query) { $uri.Query } else { "?sslmode=require" })"
    }
}

function Invoke-DockerPostgres {
    param(
        [string[]]$Arguments,
        [string]$Mount = ""
    )

    $dockerArgs = @("run", "--rm")
    if ($Mount) {
        $dockerArgs += @("-v", $Mount)
    }
    $dockerArgs += @($PostgresImage)
    $dockerArgs += $Arguments

    & docker @dockerArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Docker PostgreSQL command failed with exit code $LASTEXITCODE."
    }
}

function Invoke-DbSql {
    param(
        [string]$Url,
        [string]$Sql
    )

    Invoke-DockerPostgres -Arguments @("psql", $Url, "-v", "ON_ERROR_STOP=1", "-c", $Sql)
}

function Test-DockerReady {
    & docker version --format "{{.Server.Version}}" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Desktop is not running. Start Docker Desktop and run this script again."
    }
}

if (-not $SourceUrl) {
    $SourceUrl = Read-SecretText "Render External Database URL"
}

if (-not $TargetUrl) {
    $TargetUrl = Read-SecretText "Neon connection string"
}

$sourceInfo = Get-PostgresUrlInfo $SourceUrl
$targetInfo = Get-PostgresUrlInfo $TargetUrl

if ($sourceInfo.Host -notmatch "\.") {
    Write-Warning "The Render host '$($sourceInfo.Host)' looks like an internal Render hostname. Use the External Database URL from the Render database dashboard."
}

Test-DockerReady

New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

if (-not $BackupFile) {
    $BackupFile = "render-to-neon-$(Get-Date -Format yyyyMMdd-HHmmss).dump"
}

$mount = "${BackupDir}:/backups"
$backupInContainer = "/backups/$BackupFile"

Write-Host "Source host: $($sourceInfo.Host)"
Write-Host "Target host: $($targetInfo.Host)"
Write-Host "PostgreSQL Docker image: $PostgresImage"
Write-Host "Backup file: $(Join-Path $BackupDir $BackupFile)"

Write-Host ""
Write-Host "Measuring source database..."
Invoke-DbSql -Url $SourceUrl -Sql "select pg_size_pretty(pg_database_size(current_database())) as database_size; select version();"

if (-not $ValidateOnly -and -not $SkipDump) {
    Write-Host ""
    Write-Host "Creating dump..."
    Invoke-DockerPostgres -Mount $mount -Arguments @(
        "pg_dump",
        $SourceUrl,
        "--format=custom",
        "--no-owner",
        "--no-acl",
        "--file=$backupInContainer"
    )
}

if (-not $ValidateOnly -and -not $SkipRestore) {
    Write-Host ""
    Write-Host "Restoring dump into Neon..."
    Invoke-DockerPostgres -Mount $mount -Arguments @(
        "pg_restore",
        "--clean",
        "--if-exists",
        "--no-owner",
        "--no-acl",
        "--dbname",
        $TargetUrl,
        $backupInContainer
    )
}

Write-Host ""
Write-Host "Validating Neon counts..."
Invoke-DbSql -Url $TargetUrl -Sql @"
select 'decks' as table_name, count(*) as row_count from decks
union all select 'deck_cards', count(*) from deck_cards
union all select 'deck_likes', count(*) from deck_likes
union all select 'recommendation_audit_runs', count(*) from recommendation_audit_runs
union all select 'meta_top_decks', count(*) from meta_top_decks
union all select 'meta_combos', count(*) from meta_combos
union all select 'flyway_schema_history', count(*) from flyway_schema_history
order by table_name;
"@

Write-Host ""
Write-Host "Render environment variables for Neon:"
Write-Host "QUARKUS_DATASOURCE_DB_KIND=postgresql"
Write-Host "QUARKUS_DATASOURCE_JDBC_URL=$($targetInfo.JdbcUrl)"
Write-Host "QUARKUS_DATASOURCE_USERNAME=$($targetInfo.User)"
Write-Host "QUARKUS_DATASOURCE_PASSWORD=<use Neon password>"
Write-Host "QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY=validate"
Write-Host "QUARKUS_FLYWAY_MIGRATE_AT_START=false"
Write-Host "QUARKUS_DATASOURCE_JDBC_MAX_SIZE=5"
