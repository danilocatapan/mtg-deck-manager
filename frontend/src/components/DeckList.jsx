import React from 'react'

export default function DeckList({ decks = [] }) {
  if (!decks || decks.length === 0) return <p>No decks found</p>

  return (
    <ul>
      {decks.map((d) => (
        <li key={d.id} style={{ textAlign: 'left', margin: '8px 0' }}>
          <strong>{d.name}</strong> — <span>{d.commander}</span>
        </li>
      ))}
    </ul>
  )
}
