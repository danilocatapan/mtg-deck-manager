import React, { useEffect, useState } from 'react'
import DeckForm from '../components/DeckForm'
import { createDeck, updateDeck } from '../services/api'

export default function DeckEditorPage({ mode = 'create', deck = null, onDone }) {
  const [initial, setInitial] = useState(null)

  useEffect(() => {
    if (mode === 'edit' && deck) setInitial(deck)
    if (mode === 'create') setInitial(null)
  }, [mode, deck])

  async function handleSave(payload) {
    try {
      if (mode === 'create') {
        const created = await createDeck(payload)
        console.log('Deck created', created)
      } else {
        const updated = await updateDeck(deck.id, payload)
        console.log('Deck updated', updated)
      }
      onDone && onDone()
    } catch (e) {
      console.error('save error', e)
      alert('Failed to save deck')
    }
  }

  return (
    <section style={{ padding: 20 }}>
      <h2>{mode === 'create' ? 'Create Deck' : 'Edit Deck'}</h2>
      <DeckForm initial={initial} onCancel={onDone} onSave={handleSave} />
    </section>
  )
}
