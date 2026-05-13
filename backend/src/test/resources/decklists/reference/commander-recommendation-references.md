# Commander Recommendation Reference Decks

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
- Examples: Nature's Lore, Greater Good, Heroic Intervention, Beast Within.
- Off-color staples must not be recommended.
