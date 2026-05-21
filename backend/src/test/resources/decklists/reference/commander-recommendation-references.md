# Commander Recommendation Reference Decks

Versao docs: 2026-05-21
Ultima atualizacao: 2026-05-21

Offline notes for recommendation regression tests. Tests should keep using local fixtures and mocks; these links are context for the archetypes and card choices.

## cEDH: Tymna/Kraum Blue Farm

Sources:
- https://learncedh.com/decklists/tymna-kraum
- https://cedh-decklist-database.com/
- https://cedhstats.org/commanders/kraum-tymna

Signals used by tests:
- cEDH recommendations should favor cheap interaction, early draw engines, fast mana, tutors, and compact win conditions.
- Examples: Mystic Remora, Rhystic Study, Esper Sentinel, Silence effects, cheap counters, Underworld Breach, Thassa's Oracle lines.
- Slow value cards and expensive tutors are weaker cuts for cEDH-style goals.

## Casual/Mid/High-Power: Xenagos, God of Revels

Sources:
- https://edhrec.com/average-decks/xenagos-god-of-revels
- https://edhrec.com/average-decks/xenagos-god-of-revels/core
- https://edhrec.com/average-decks/xenagos-god-of-revels/upgraded
- https://edhrec.com/average-decks/xenagos-god-of-revels/optimized

Signals used by tests:
- Xenagos recommendations should preserve Gruul combat/ramp identity.
- High-power Xenagos recommendations should lower curve pressure, add explosive mana, add extra-combat/combo redundancy, and improve multiplayer closing power.
- Examples: Savage Ventmaw, Bloodthirster, Scourge of the Throne, Hellkite Charger, Finale of Devastation, Greater Good, Worldly Tutor, Utopia Sprawl.
- Weak cuts for the benchmark include slow ramp, tapped lands, conditional draw, and expensive threats that do not immediately close the table.
- Examples: Terastodon, Thran Dynamo, Nissa, Who Shakes the World, Garruk, Primal Hunter, Siege Behemoth, Cultivate, Nissa's Pilgrimage, Temple of Abandon, Rugged Highlands, Soul's Majesty.
- Off-color staples must not be recommended.

## Cross-Archetype Benchmarks

Signals used by tests:
- Recommendation quality is judged by archetype and bracket, not by commander-specific hard-coded rules.
- Control/stax lists should favor cheap interaction, early draw engines, efficient stax pieces, and cuts to slow generic value.
- Turbo-combo/cEDH lists should favor compact win conditions, cheap tutors, fast mana, stack interaction, and one-card-away combo completion.
- Tokens, aristocrats, reanimator, spellslinger, voltron, combat, midrange, and value lists should receive candidates from their structural plan while preserving Commander invariants.
- Meta top deck signals may influence ranking only when sample, format, bracket/source and color identity filters are valid.
- Apply/undo swap tests should preserve deck size, avoid commander cuts and keep recommendation audit data coherent.
