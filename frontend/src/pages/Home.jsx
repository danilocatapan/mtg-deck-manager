import React, { useEffect, useState } from 'react'
import { fetchDecks, deleteDeck } from '../services/api'
import DeckList from '../components/DeckList'
import CardSearch from '../components/CardSearch'
import DeckEditorPage from './DeckEditorPage'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'

export default function Home() {
  const [decks, setDecks] = useState([])
  const [view, setView] = useState('home') // 'home' | 'create' | 'edit'
  const [editingDeck, setEditingDeck] = useState(null)

  async function load() {
    const d = await fetchDecks()
    setDecks(d)
  }

  useEffect(() => {
    let mounted = true
    fetchDecks().then((d) => {
      if (mounted) setDecks(d)
    })
    return () => (mounted = false)
  }, [])

  function handleCreate() {
    setEditingDeck(null)
    setView('create')
  }

  function handleEdit(deck) {
    setEditingDeck(deck)
    setView('edit')
  }

  async function handleDelete(deck) {
    if (!confirm(`Delete deck ${deck.name}?`)) return
    try {
      await deleteDeck(deck.id)
      await load()
    } catch (e) {
      console.error('delete failed', e)
      alert('Delete failed')
    }
  }

  function handleDone() {
    setView('home')
    load()
  }

  if (view === 'create' || view === 'edit') {
    return <DeckEditorPage mode={view === 'create' ? 'create' : 'edit'} deck={editingDeck} onDone={handleDone} />
  }

  return (
    <main>
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ margin: 0 }}>Decks</h1>
        <div>
          <Button onClick={handleCreate}>Create Deck</Button>
        </div>
      </div>

      <Card>
        <DeckList decks={decks} onEdit={handleEdit} onDelete={handleDelete} />
      </Card>

      <h2 style={{ marginTop: 24 }}>Search Cards</h2>
      <Card>
        <CardSearch />
      </Card>
    </main>
  )
}
