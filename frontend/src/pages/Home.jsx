import React, { useEffect, useState } from 'react'
import { deleteDeck, fetchDecks } from '../services/api'
import DeckList from '../components/DeckList'
import DeckEditorPage from './DeckEditorPage'
import ImportDeckPage from './ImportDeckPage'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import createIcon from '../assets/icons/create.png'
import importIcon from '../assets/icons/import.png'

export default function Home() {
  const [decks, setDecks] = useState([])
  const [view, setView] = useState('home')
  const [editingDeck, setEditingDeck] = useState(null)
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState(null)

  async function load() {
    setLoading(true)
    const loadedDecks = await fetchDecks()
    setDecks(loadedDecks)
    setLoading(false)
  }

  useEffect(() => {
    let mounted = true
    fetchDecks().then((loadedDecks) => {
      if (mounted) {
        setDecks(loadedDecks)
        setLoading(false)
      }
    })
    return () => {
      mounted = false
    }
  }, [])

  function handleCreate() {
    setMessage(null)
    setEditingDeck(null)
    setView('create')
  }

  function handleImport() {
    setMessage(null)
    setEditingDeck(null)
    setView('import')
  }

  function handleEdit(deck) {
    setMessage(null)
    setEditingDeck(deck)
    setView('edit')
  }

  async function handleDelete(deck) {
    if (!confirm(`Delete deck ${deck.name}?`)) return
    try {
      await deleteDeck(deck.id)
      setMessage(`Deleted ${deck.name}.`)
      await load()
    } catch (e) {
      console.error('delete failed', e)
      setMessage('Delete failed. Try again.')
    }
  }

  function handleDone(nextMessage) {
    setView('home')
    setMessage(nextMessage || null)
    load()
  }

  if (view === 'create' || view === 'edit') {
    return <DeckEditorPage mode={view === 'create' ? 'create' : 'edit'} deck={editingDeck} onDone={handleDone} />
  }

  if (view === 'import') {
    return <ImportDeckPage onDone={handleDone} />
  }

  return (
    <main>
      <section className="page-heading">
        <div>
          <p className="eyebrow">Commander workflow</p>
          <h1>Decks</h1>
          <p className="page-description">Create or import a deck, tune the list, analyze structure, then request explainable recommendations.</p>
        </div>
        <div className="actions-row">
          <Button onClick={handleCreate}>
            <img className="btn-icon" src={createIcon} alt="" aria-hidden="true" />
            Create Deck
          </Button>
          <Button variant="secondary" onClick={handleImport}>
            <img className="btn-icon" src={importIcon} alt="" aria-hidden="true" />
            Import Deck
          </Button>
        </div>
      </section>

      <Card>
        <div className="workflow-steps" aria-label="Main workflow">
          <div><strong>1</strong><span>Create or import</span></div>
          <div><strong>2</strong><span>Edit card list</span></div>
          <div><strong>3</strong><span>Analyze deck</span></div>
          <div><strong>4</strong><span>Optimize recommendations</span></div>
        </div>
      </Card>

      {message && <div className="status success">{message}</div>}
      {loading ? (
        <Card><div className="loading">Loading decks...</div></Card>
      ) : (
        <Card>
          <DeckList
            decks={decks}
            onEdit={handleEdit}
            onDelete={handleDelete}
            onCreate={handleCreate}
            onImport={handleImport}
          />
        </Card>
      )}
    </main>
  )
}
