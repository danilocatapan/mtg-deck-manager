import React, { useState, useEffect } from 'react'
import CardSearch from './CardSearch'

export default function DeckForm({ initial = null, onCancel, onSave }) {
  const [name, setName] = useState('')
  const [commander, setCommander] = useState('')
  const [cards, setCards] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    if (initial) {
      setName(initial.name || '')
      setCommander(initial.commander || '')
      setCards(initial.cards ? initial.cards.map(c => ({ name: c.name, quantity: c.quantity })) : [])
    }
  }, [initial])

  function addCard(card) {
    setCards((prev) => {
      const found = prev.find((p) => p.name === card.name)
      if (found) return prev.map((p) => (p.name === card.name ? { ...p, quantity: p.quantity + 1 } : p))
      return [...prev, { name: card.name, quantity: 1 }]
    })
  }

  function removeCard(name) {
    setCards((prev) => prev.filter((c) => c.name !== name))
  }

  function changeQuantity(name, qty) {
    setCards((prev) => prev.map((c) => (c.name === name ? { ...c, quantity: Math.max(1, qty) } : c)))
  }

  function validate() {
    if (!name || !commander) {
      setError('Name and commander are required')
      return false
    }
    if (!cards || cards.length === 0) {
      setError('Add at least one card')
      return false
    }
    setError(null)
    return true
  }

  function handleSubmit(e) {
    e.preventDefault()
    if (!validate()) return
    const payload = {
      name,
      commander,
      cards: cards.map((c) => ({ name: c.name, quantity: c.quantity })),
    }
    onSave && onSave(payload)
  }

  return (
    <div style={{ textAlign: 'left' }}>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Deck name</label>
          <br />
          <input value={name} onChange={(e) => setName(e.target.value)} />
        </div>

        <div style={{ marginTop: 8 }}>
          <label>Commander</label>
          <br />
          <input value={commander} onChange={(e) => setCommander(e.target.value)} />
        </div>

        <div style={{ marginTop: 12 }}>
          <label>Add cards</label>
          <CardSearch onSelect={addCard} />
        </div>

        <div style={{ marginTop: 12 }}>
          <label>Cards</label>
          <ul>
            {cards.map((c) => (
              <li key={c.name} style={{ marginBottom: 6 }}>
                <strong>{c.name}</strong>{' '}
                <input
                  type="number"
                  value={c.quantity}
                  min={1}
                  onChange={(e) => changeQuantity(c.name, parseInt(e.target.value || '1', 10))}
                  style={{ width: 60, marginLeft: 8 }}
                />
                <button type="button" onClick={() => removeCard(c.name)} style={{ marginLeft: 8 }}>
                  Remove
                </button>
              </li>
            ))}
          </ul>
        </div>

        {error && <div style={{ color: 'red' }}>{error}</div>}

        <div style={{ marginTop: 12 }}>
          <button type="submit">Save</button>
          <button type="button" onClick={onCancel} style={{ marginLeft: 8 }}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
