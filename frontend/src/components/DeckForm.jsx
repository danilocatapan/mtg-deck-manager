import React, { useEffect, useMemo, useState } from 'react'
import CardSearch from './CardSearch'
import Button from './ui/Button'

export default function DeckForm({ initial = null, onCancel, onSave }) {
  const [name, setName] = useState('')
  const [commander, setCommander] = useState('')
  const [cards, setCards] = useState([])
  const [error, setError] = useState(null)
  const [savedMessage, setSavedMessage] = useState(null)

  useEffect(() => {
    if (initial) {
      setName(initial.name || '')
      setCommander(initial.commander || '')
      setCards(initial.cards ? initial.cards.map((card) => ({ name: card.name, quantity: card.quantity })) : [])
    }
  }, [initial])

  const totalCards = useMemo(() => cards.reduce((sum, card) => sum + Number(card.quantity || 0), 0), [cards])
  const isOverLimit = totalCards > 99
  const isValid = Boolean(name.trim() && commander.trim() && cards.length > 0 && !isOverLimit)
  const commanderInitials = commander
    .split(/[,\s]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || 'C'

  function addCard(card) {
    setSavedMessage(null)
    setCards((prev) => {
      const found = prev.find((item) => item.name === card.name)
      if (found) return prev.map((item) => (item.name === card.name ? { ...item, quantity: item.quantity + 1 } : item))
      return [...prev, { name: card.name, quantity: 1 }]
    })
  }

  function removeCard(nameToRemove) {
    setSavedMessage(null)
    setCards((prev) => prev.filter((card) => card.name !== nameToRemove))
  }

  function changeQuantity(cardName, qty) {
    setSavedMessage(null)
    setCards((prev) => prev.map((card) => (card.name === cardName ? { ...card, quantity: Math.max(1, qty) } : card)))
  }

  function validate() {
    if (!name.trim() || !commander.trim()) {
      setError('Deck name and commander are required.')
      return false
    }
    if (cards.length === 0) {
      setError('Add at least one card before saving.')
      return false
    }
    if (isOverLimit) {
      setError(`Commander decks can have up to 99 cards outside the commander. Current list has ${totalCards}.`)
      return false
    }
    setError(null)
    return true
  }

  function handleSubmit(e) {
    e.preventDefault()
    if (!validate()) return

    const payload = {
      name: name.trim(),
      commander: commander.trim(),
      cards: cards.map((card) => ({ name: card.name, quantity: card.quantity })),
    }
    setSavedMessage('Saving deck...')
    onSave && onSave(payload)
  }

  return (
    <form onSubmit={handleSubmit} className="deck-editor-form">
      <section className="commander-card">
        <div className="commander-sigil" aria-hidden="true">{commanderInitials}</div>
        <div className="commander-details">
          <p className="eyebrow">Commander Identity</p>
          <h2>{commander.trim() || 'Choose your commander'}</h2>
          <div className="commander-meta">
            <span>{commander.trim() ? 'Commander legal check pending' : 'Name defines color identity and recommendations'}</span>
            <span>{name.trim() || 'Untitled deck'}</span>
          </div>
        </div>
      </section>

      <div className="form-grid">
        <label>
          Deck name
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Gruul Revels" />
        </label>

        <label>
          Commander
          <input value={commander} onChange={(e) => setCommander(e.target.value)} placeholder="Xenagos, God of Revels" />
        </label>
      </div>

      <div className="deck-health">
        <div>
          <strong className={isOverLimit ? 'is-invalid' : ''}>{totalCards}/99</strong>
          <span> cards in deck</span>
        </div>
        <div className={isValid ? 'status-pill ready' : 'status-pill'}>
          {isValid ? 'Ready to save' : isOverLimit ? 'Over Commander limit' : 'Needs name, commander and cards'}
        </div>
      </div>

      <section className="editor-section">
        <div className="section-heading">
          <div>
            <h3>Add cards</h3>
            <p>Search a card and use Add to place it directly into this deck.</p>
          </div>
        </div>
        <CardSearch onSelect={addCard} />
      </section>

      <section className="editor-section">
        <div className="section-heading">
          <div>
            <h3>Deck list</h3>
            <p>Adjust quantities or remove cards before saving.</p>
          </div>
        </div>

        {cards.length === 0 ? (
          <div className="empty-inline">No cards added yet. Search above to start building.</div>
        ) : (
          <div className="deck-table">
            {cards.map((card) => (
              <div key={card.name} className="deck-row">
                <strong>{card.name}</strong>
                <input
                  aria-label={`Quantity for ${card.name}`}
                  type="number"
                  value={card.quantity}
                  min={1}
                  onChange={(e) => changeQuantity(card.name, parseInt(e.target.value || '1', 10))}
                />
                <Button type="button" variant="danger" onClick={() => removeCard(card.name)}>
                  Remove
                </Button>
              </div>
            ))}
          </div>
        )}
      </section>

      {error && <div className="status error">{error}</div>}
      {savedMessage && <div className="status">{savedMessage}</div>}

      <div className="form-actions">
        <Button type="submit" disabled={!isValid}>Save Deck</Button>
        <Button type="button" variant="secondary" onClick={onCancel}>Back</Button>
      </div>
    </form>
  )
}
