param(
    [ValidateRange(1, 50)]
    [int]$Target = 25,
    [ValidateRange(1, 365)]
    [int]$Days = 180,
    [ValidateRange(1, 1000)]
    [int]$MinParticipants = 32,
    [string]$OutputPath = "backend/src/main/resources/recommendation-benchmark/topdeck-snapshots.json"
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($env:TOPDECK_API_KEY)) {
    throw "TOPDECK_API_KEY is required to collect competitive benchmark snapshots."
}

$capturedAt = [DateTimeOffset]::UtcNow.ToString("o")
$headers = @{ Authorization = $env:TOPDECK_API_KEY; "Content-Type" = "application/json" }
$body = @{
    game = "Magic: The Gathering"
    format = "EDH"
    last = $Days
    participantMin = $MinParticipants
    columns = @("name", "commander", "deckObj", "decklist", "wins", "draws", "losses", "winRate")
} | ConvertTo-Json
$tournaments = @(Invoke-RestMethod -Method Post -Uri "https://topdeck.gg/api/v2/tournaments" -Headers $headers -Body $body)
$ranked = [System.Collections.Generic.List[object]]::new()

foreach ($tournament in $tournaments) {
    $placement = 0
    foreach ($standing in @($tournament.standings)) {
        $placement++
        if ([string]::IsNullOrWhiteSpace($standing.commander) -or $null -eq $standing.deckObj) { continue }
        $total = 1
        foreach ($quantity in $standing.deckObj.PSObject.Properties.Value) { $total += [int]$quantity }
        if ($total -ne 100) { continue }
        $ranked.Add([ordered]@{
            tournament = $tournament
            standing = $standing
            placement = $placement
            participantCount = [int]$tournament.participantCount
            winRate = [double]$standing.winRate
        })
    }
}

$selected = [System.Collections.Generic.List[object]]::new()
$commanders = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
foreach ($entry in @($ranked | Sort-Object @{ Expression = "placement"; Ascending = $true }, @{ Expression = "winRate"; Descending = $true }, @{ Expression = "participantCount"; Descending = $true })) {
    if ($selected.Count -ge $Target) { break }
    if (-not $commanders.Add($entry.standing.commander)) { continue }
    $deck = @([ordered]@{ name = $entry.standing.commander; quantity = 1 })
    foreach ($card in $entry.standing.deckObj.PSObject.Properties) {
        $deck += [ordered]@{ name = $card.Name; quantity = [int]$card.Value }
    }
    $selected.Add([ordered]@{
        id = "topdeck-$($entry.tournament.id)-$($entry.placement)"
        commander = $entry.standing.commander
        bracket = if ($entry.placement -le 16) { "cedh" } else { "high-power" }
        strategy = "cedh"
        deck = $deck
        catalog = @{}
        meta = @()
        preferences = [ordered]@{}
        filters = @()
        labels = [ordered]@{ protectedCards = @($entry.standing.commander); expectedAdds = @(); expectedCuts = @() }
        source = "topdeck_tournament"
        sourceUrl = $entry.tournament.topdeckUrl
        capturedAt = $capturedAt
        provenance = [ordered]@{
            source = "topdeck_tournament"
            sourceUrl = $entry.tournament.topdeckUrl
            capturedAt = $capturedAt
            tournament = $entry.tournament.tournamentName
            participants = $entry.participantCount
            placement = $entry.placement
            winRate = $entry.winRate
        }
    })
}

if ($selected.Count -lt $Target) {
    throw "Only $($selected.Count) complete TopDeck.gg decks with distinct commanders were found."
}

$resolved = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputPath))
[ordered]@{
    source = "TopDeck.gg"
    capturedAt = $capturedAt
    count = $selected.Count
    cases = $selected
    qualificationNote = "Catalog metadata must be frozen before these cases become benchmark-qualifiable."
} | ConvertTo-Json -Depth 12 | Set-Content -Encoding utf8 $resolved

Write-Output "Collected $($selected.Count) TopDeck.gg snapshots at $resolved"
