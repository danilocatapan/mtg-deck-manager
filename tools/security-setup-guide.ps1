param(
    [switch]$ShowExamples
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "== $Title ==" -ForegroundColor Cyan
}

function Write-Check {
    param(
        [string]$Name,
        [string]$Scope,
        [string]$Why,
        [bool]$Required = $true
    )

    $value = [Environment]::GetEnvironmentVariable($Name)
    $configured = -not [string]::IsNullOrWhiteSpace($value)
    $status = if ($configured) { "configured" } elseif ($Required) { "missing" } else { "optional" }
    $color = if ($configured) { "Green" } elseif ($Required) { "Yellow" } else { "DarkGray" }

    Write-Host ("[{0}] {1}" -f $status, $Name) -ForegroundColor $color
    Write-Host ("  Scope: {0}" -f $Scope)
    Write-Host ("  Why:   {0}" -f $Why)
}

Write-Host "MTG Deck Manager - Security Setup Guide" -ForegroundColor Green
Write-Host "This script checks whether security-related environment variables are present."
Write-Host "It never prints secret values, tokens, Google subjects or database passwords."

Write-Section "Required for the security status endpoint"
Write-Check `
    -Name "SECURITY_ADMIN_SUBJECTS" `
    -Scope "Backend deploy secret/environment variable" `
    -Why "Comma-separated Google subject IDs allowed to call POST /security/status/check. Keep real values out of GitHub."

Write-Check `
    -Name "GOOGLE_CLIENT_ID" `
    -Scope "Backend deploy environment variable" `
    -Why "Lets Quarkus validate Google ID tokens sent as Bearer tokens."

Write-Section "Required for frontend login/build"
Write-Check `
    -Name "VITE_GOOGLE_CLIENT_ID" `
    -Scope "Frontend build variable" `
    -Why "Enables Google Sign-In in the React app. This is public by design, but restrict it in Google Console."

Write-Check `
    -Name "VITE_API_URL" `
    -Scope "Frontend build variable" `
    -Why "Points the React app to the deployed backend API."

Write-Section "Recommended hardening"
Write-Check `
    -Name "CORS_ORIGINS" `
    -Scope "Backend deploy environment variable" `
    -Why "Restricts browser access to known frontend origins; avoid wildcard origins in production."

Write-Check `
    -Name "APP_ENVIRONMENT" `
    -Scope "Backend deploy environment variable" `
    -Why "Set to production in production so diagnostic details are redacted."

Write-Check `
    -Name "SECURITY_STATUS_LOG_LEVEL" `
    -Scope "Backend deploy environment variable" `
    -Why "Controls logs for SecurityResource/SecurityStatusService. Default is INFO." `
    -Required $false

Write-Section "What was implemented"
Write-Host "- POST /security/status/check is an authenticated, admin-only, read-only diagnostic endpoint."
Write-Host "- It reports safe security posture signals: CORS, OIDC/client id, admin allowlist, headers and dependency scan mode."
Write-Host "- It does not change decks, recommendations, scores, users, runtime config or database state."
Write-Host "- Production responses redact sensitive details even when includeDetails=true."
Write-Host "- Frontend session handling validates token issuer/audience/expiration and clears session on 401."
Write-Host "- Logs explain the flow with event names and counts, without tokens, secrets, full payloads or personal data."

Write-Section "How to call after configuration"
Write-Host "POST /security/status/check"
Write-Host "Authorization: Bearer <google-id-token>"
Write-Host "Content-Type: application/json"
Write-Host ""
Write-Host "{"
Write-Host '  "includeDetails": true,'
Write-Host '  "scanExternalDependencies": false'
Write-Host "}"

Write-Section "Public GitHub safety rules"
Write-Host "- Commit variable names and placeholders only."
Write-Host "- Do not commit real SECURITY_ADMIN_SUBJECTS values; treat them as admin identifiers."
Write-Host "- Do not commit .env with real deploy credentials."
Write-Host "- Keep GOOGLE_CLIENT_ID and VITE_GOOGLE_CLIENT_ID restricted to authorized origins in Google Console."
Write-Host "- Keep dependency scans in CI/SBOM; the endpoint intentionally avoids external runtime scanning."

if ($ShowExamples) {
    Write-Section "Local examples"
    Write-Host '$env:SECURITY_ADMIN_SUBJECTS = "google-subject-id-1,google-subject-id-2"'
    Write-Host '$env:GOOGLE_CLIENT_ID = "your-google-client-id.apps.googleusercontent.com"'
    Write-Host '$env:VITE_GOOGLE_CLIENT_ID = "your-google-client-id.apps.googleusercontent.com"'
    Write-Host '$env:VITE_API_URL = "http://localhost:8080"'
    Write-Host '$env:CORS_ORIGINS = "http://localhost:5173,https://danilocatapan.github.io"'
    Write-Host '$env:APP_ENVIRONMENT = "production"'
}

Write-Host ""
Write-Host "Done. Configure missing required values in your deploy provider or CI secrets, not in tracked files." -ForegroundColor Green
