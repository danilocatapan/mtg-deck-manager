import React from 'react'
import Button from './ui/Button'

export default function DeckList({ decks = [], onEdit, onDelete, onCreate, onImport }) {
  if (!decks || decks.length === 0) {
    return (
      <div className="empty-state">
        <h3>No decks yet</h3>
        <p>Create a new Commander deck from scratch or import a text list to start analyzing and optimizing.</p>
        <div className="actions-row">
          <Button onClick={onCreate}>Create Deck</Button>
          <Button variant="secondary" onClick={onImport}>Import Deck</Button>
        </div>
      </div>
    )
  }

  return (
    <div className="deck-list">
      {decks.map((deck) => {
        const totalCards = deck.cards?.reduce((sum, card) => sum + Number(card.quantity || 0), 0) ?? 0

        return (
          <div key={deck.id} className="deck-card">
            <div>
              <div className="deck-title">{deck.name}</div>
              <div className="deck-subtitle">{deck.commander}</div>
              <div className={`deck-count ${totalCards > 99 ? 'is-invalid' : ''}`}>{totalCards}/99 cards</div>
            </div>
            <div className="actions-row">
              <Button variant="secondary" onClick={() => onEdit && onEdit(deck)}>Edit</Button>
              <Button variant="danger" onClick={() => onDelete && onDelete(deck)}>Delete</Button>
            </div>
          </div>
        )
      })}
    </div>
  )
}
