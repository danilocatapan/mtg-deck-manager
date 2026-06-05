param(
    [ValidateRange(1, 50)]
    [int]$Target = 25,
    [string]$OutputPath = "backend/src/main/resources/recommendation-benchmark/archidekt-candidates.json",
    [string]$SnapshotPath = "backend/src/main/resources/recommendation-benchmark/archidekt-snapshots.json"
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
$snapshots = [System.Collections.Generic.List[object]]::new()
$commanders = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
$capturedAt = [DateTimeOffset]::UtcNow.ToString("o")

function Convert-ColorIdentity($colors) {
    $symbols = @{ White = "W"; Blue = "U"; Black = "B"; Red = "R"; Green = "G"; Colorless = "C" }
    return @($colors | ForEach-Object { if ($symbols.ContainsKey([string]$_)) { $symbols[[string]$_] } else { [string]$_ } })
}

function Convert-Bracket($value) {
    switch ([string]$value) {
        "1" { return "casual" }
        "2" { return "mid" }
        "3" { return "high-power" }
        "4" { return "cedh" }
        "casual" { return "casual" }
        "mid" { return "mid" }
        "high-power" { return "high-power" }
        "cedh" { return "cedh" }
        default { return "mid" }
    }
}

function Get-TypeLine($oracle) {
    $main = @($oracle.superTypes) + @($oracle.types)
    $line = ($main | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join " "
    $subTypes = @($oracle.subTypes) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    if ($subTypes.Count -gt 0) { $line += " - " + ($subTypes -join " ") }
    return $line
}

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
        bracket = Convert-Bracket $candidate.edhBracket
        views = $candidate.viewCount
        cards = $cardCount
        source = "archidekt_popular"
        sourceUrl = $deckUrl
        capturedAt = $capturedAt
    })
    $deck = @($detail.cards | ForEach-Object {
        [ordered]@{ name = $_.card.oracleCard.name; quantity = [int]$_.quantity }
    })
    $catalog = [System.Collections.Generic.List[object]]::new()
    foreach ($entry in $detail.cards) {
        $oracle = $entry.card.oracleCard
        $catalog.Add([ordered]@{
            name = $oracle.name
            colorIdentity = @(Convert-ColorIdentity $oracle.colorIdentity)
            typeLine = Get-TypeLine $oracle
            cmc = [double]$oracle.cmc
            oracleText = $oracle.text
        })
    }
    $snapshots.Add([ordered]@{
        id = "archidekt-$($candidate.id)"
        commander = $commander
        bracket = Convert-Bracket $candidate.edhBracket
        strategy = "consistency"
        colorIdentity = @(Convert-ColorIdentity $commanderCards[0].card.oracleCard.colorIdentity)
        deck = $deck
        catalog = $catalog
        meta = @()
        preferences = [ordered]@{}
        filters = @()
        labels = [ordered]@{ protectedCards = @($commander); expectedAdds = @(); expectedCuts = @() }
        source = "archidekt_popular"
        sourceUrl = $deckUrl
        capturedAt = $capturedAt
        provenance = [ordered]@{ source = "archidekt_popular"; sourceUrl = $deckUrl; capturedAt = $capturedAt; views = $candidate.viewCount }
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

$snapshotResolved = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $SnapshotPath))
[ordered]@{
    source = "Archidekt"
    capturedAt = $capturedAt
    count = $snapshots.Count
    cases = $snapshots
} | ConvertTo-Json -Depth 12 | Set-Content -Encoding utf8 $snapshotResolved

Write-Output "Collected $($selected.Count) Archidekt candidates at $resolved and full snapshots at $snapshotResolved"
