import React from 'react'

export default function DeckList({ decks = [], onEdit, onDelete }) {
  if (!decks || decks.length === 0) return <p>No decks found</p>

  return (
    <ul>
      {decks.map((d) => (
        <li key={d.id} style={{ textAlign: 'left', margin: '8px 0' }}>
          <strong>{d.name}</strong> — <span>{d.commander}</span>
          <button onClick={() => onEdit && onEdit(d)} style={{ marginLeft: 8 }}>Edit</button>
          <button onClick={() => onDelete && onDelete(d)} style={{ marginLeft: 8 }}>Delete</button>
        </li>
      ))}
    </ul>
  )
}
