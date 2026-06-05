param(
    [ValidateRange(1, 50)]
    [int]$Target = 25,
    [string]$OutputPath = "backend/src/main/resources/recommendation-benchmark/archidekt-candidates.json"
)

$ErrorActionPreference = "Stop"
$searchUrl = "https://archidekt.com/search/decks?deckFormat=3&orderBy=-viewCount"
$html = (Invoke-WebRequest -UseBasicParsing $searchUrl).Content
$match = [regex]::Match($html, '<script id="__NEXT_DATA__" type="application/json">(.*?)</script>')
if (-not $match.Success) {
    throw "Archidekt ranking payload not found."
}

$ranking = ($match.Groups[1].Value | ConvertFrom-Json).props.pageProps.deckResults.results
$selected = [System.Collections.Generic.List[object]]::new()
$commanders = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
$capturedAt = [DateTimeOffset]::UtcNow.ToString("o")

foreach ($candidate in $ranking) {
    if ($selected.Count -ge $Target) {
        break
    }
    if ($candidate.private -or $candidate.unlisted -or $candidate.size -ne 100) {
        continue
    }

    $deckUrl = "https://archidekt.com/decks/$($candidate.id)"
    $detail = (Invoke-WebRequest -UseBasicParsing "https://archidekt.com/api/decks/$($candidate.id)/").Content | ConvertFrom-Json
    $commanderCards = @($detail.cards | Where-Object { $_.categories -contains "Commander" })
    if ($commanderCards.Count -ne 1) {
        continue
    }

    $commander = $commanderCards[0].card.oracleCard.name
    $cardCount = ($detail.cards | Measure-Object -Property quantity -Sum).Sum
    if ([string]::IsNullOrWhiteSpace($commander) -or $cardCount -ne 100 -or -not $commanders.Add($commander)) {
        continue
    }

    $selected.Add([ordered]@{
        rank = $selected.Count + 1
        archidektId = $candidate.id
        name = $candidate.name
        commander = $commander
        bracket = $candidate.edhBracket
        views = $candidate.viewCount
        cards = $cardCount
        source = "archidekt_popular"
        sourceUrl = $deckUrl
        capturedAt = $capturedAt
    })
    Start-Sleep -Milliseconds 150
}

if ($selected.Count -lt $Target) {
    throw "Only $($selected.Count) complete decks with distinct commanders were found."
}

$resolved = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputPath))
$parent = Split-Path -Parent $resolved
if (-not (Test-Path $parent)) {
    New-Item -ItemType Directory -Path $parent | Out-Null
}

[ordered]@{
    source = "Archidekt"
    selection = "Commander decks ordered by descending view count, complete and deduplicated by commander"
    rankingUrl = $searchUrl
    capturedAt = $capturedAt
    count = $selected.Count
    decks = $selected
} | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 $resolved

Write-Output "Collected $($selected.Count) Archidekt candidates at $resolved"
