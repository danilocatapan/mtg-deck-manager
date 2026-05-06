import { useEffect, useState } from 'react'
import { deleteDeck, fetchDecks } from '../services/api'
import DeckList from '../components/DeckList'
import DeckEditorPage from './DeckEditorPage'
import ImportDeckPage from './ImportDeckPage'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import { getAuthToken, subscribeAuth } from '../services/auth'
import createIcon from '../assets/icons/create.png'
import importIcon from '../assets/icons/import.png'

export default function Home() {
  const [decks, setDecks] = useState([])
  const [view, setView] = useState('home')
  const [editingDeck, setEditingDeck] = useState(null)
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(getAuthToken()))
  const [loading, setLoading] = useState(() => isAuthenticated)
  const [message, setMessage] = useState(null)

  async function load() {
    if (!getAuthToken()) {
      setDecks([])
      setLoading(false)
      return
    }
    setLoading(true)
    const loadedDecks = await fetchDecks()
    setDecks(loadedDecks)
    setLoading(false)
  }

  useEffect(() => {
    let mounted = true
    if (!isAuthenticated) {
      return () => {
        mounted = false
      }
    }
    fetchDecks().then((loadedDecks) => {
      if (mounted) {
        setDecks(loadedDecks)
        setLoading(false)
      }
    })
    return () => {
      mounted = false
    }
  }, [isAuthenticated])

  useEffect(() => subscribeAuth(() => {
    const nextIsAuthenticated = Boolean(getAuthToken())
    setIsAuthenticated(nextIsAuthenticated)
    if (!nextIsAuthenticated) {
      setDecks([])
      setLoading(false)
    }
  }), [])

  function handleCreate() {
    if (!isAuthenticated) {
      setMessage('Sign in with Google before creating decks.')
      return
    }
    setMessage(null)
    setEditingDeck(null)
    setView('create')
  }

  function handleImport() {
    if (!isAuthenticated) {
      setMessage('Sign in with Google before importing decks.')
      return
    }
    setMessage(null)
    setEditingDeck(null)
    setView('import')
  }

  function handleEdit(deck) {
    if (!isAuthenticated) {
      setMessage('Sign in with Google before editing decks.')
      return
    }
    setMessage(null)
    setEditingDeck(deck)
    setView('edit')
  }

  async function handleDelete(deck) {
    if (!isAuthenticated) {
      setMessage('Sign in with Google before deleting decks.')
      return
    }
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
      <section className="zone zone-command page-heading">
        <div>
          <p className="eyebrow">Command Zone</p>
          <h1>Deck Library</h1>
          <p className="page-description">Create or import a deck, tune the list, analyze structure, then request explainable recommendations.</p>
        </div>
        <div className="actions-row">
          <Button className="cta-primary" onClick={handleCreate}>
            <img className="btn-icon" src={createIcon} alt="" aria-hidden="true" />
            Create Deck
          </Button>
          <Button variant="secondary" onClick={handleImport}>
            <img className="btn-icon" src={importIcon} alt="" aria-hidden="true" />
            Import Deck
          </Button>
        </div>
      </section>

      <Card className="zone zone-battlefield">
        <div className="workflow-steps" aria-label="Main workflow">
          <div data-state="active"><strong>1</strong><span>Create or import</span></div>
          <div><strong>2</strong><span>Edit library</span></div>
          <div><strong>3</strong><span>Battlefield metrics</span></div>
          <div><strong>4</strong><span>Upgrade path</span></div>
        </div>
      </Card>

      {message && <div className="status success">{message}</div>}
      {!isAuthenticated && (
        <div className="status">
          Sign in with Google to create, import, edit, list, or delete your decks.
        </div>
      )}
      {loading ? (
        <Card><div className="loading">Loading decks...</div></Card>
      ) : (
        <Card className="zone zone-library">
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
