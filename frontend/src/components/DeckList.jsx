import React from 'react'

export default function DeckList({ decks = [], onEdit, onDelete }) {
  if (!decks || decks.length === 0) return <p>No decks found</p>

  return (
    <div>
      {decks.map((d) => (
        <div key={d.id} className="deck-card">
          <div>
            <div style={{ fontWeight: 600 }}>{d.name}</div>
            <div style={{ color: 'var(--text)' }}>{d.commander}</div>
          </div>
          <div>
            <button className="btn secondary" onClick={() => onEdit && onEdit(d)} style={{ marginRight: 8 }}>Edit</button>
            <button className="btn" onClick={() => onDelete && onDelete(d)}>Delete</button>
          </div>
        </div>
      ))}
    </div>
  )
}
