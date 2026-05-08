import { useCallback, useEffect, useState } from 'react'
import { deleteDeck, fetchDecks } from '../services/api'
import DeckList from '../components/DeckList'
import DeckEditorPage from './DeckEditorPage'
import ImportDeckPage from './ImportDeckPage'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import StateMessage from '../components/ui/StateMessage'
import { getAuthToken, subscribeAuth } from '../services/auth'
import { ApiStartingError } from '../services/api'
import createIcon from '../assets/icons/create.png'
import importIcon from '../assets/icons/import.png'

export default function Home() {
  const [decks, setDecks] = useState([])
  const [view, setView] = useState('home')
  const [editingDeck, setEditingDeck] = useState(null)
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(getAuthToken()))
  const [loading, setLoading] = useState(() => isAuthenticated)
  const [message, setMessage] = useState(null)
  const [apiStatus, setApiStatus] = useState(null)

  const load = useCallback(async function loadDecks() {
    if (!getAuthToken()) {
      setDecks([])
      setLoading(false)
      setApiStatus(null)
      return
    }
    setLoading(true)
    setApiStatus(null)
    try {
      const loadedDecks = await fetchDecks({ throwOnError: true })
      setDecks(loadedDecks)
    } catch (error) {
      console.error('load decks failed', error)
      setApiStatus(error instanceof ApiStartingError ? 'starting' : 'unavailable')
      setDecks([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let mounted = true
    if (!isAuthenticated) {
      return () => {
        mounted = false
      }
    }
    fetchDecks({ throwOnError: true })
      .then((loadedDecks) => {
        if (mounted) {
          setDecks(loadedDecks)
          setApiStatus(null)
        }
      })
      .catch((error) => {
        console.error('load decks failed', error)
        if (mounted) {
          setDecks([])
          setApiStatus(error instanceof ApiStartingError ? 'starting' : 'unavailable')
        }
      })
      .finally(() => {
        if (mounted) {
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
      setApiStatus(null)
      return
    }
    load()
  }), [load])

  function handleCreate() {
    if (!isAuthenticated) {
      setMessage('Entre com Google antes de criar decks.')
      return
    }
    setMessage(null)
    setEditingDeck(null)
    setView('create')
  }

  function handleImport() {
    if (!isAuthenticated) {
      setMessage('Entre com Google antes de importar decks.')
      return
    }
    setMessage(null)
    setEditingDeck(null)
    setView('import')
  }

  function handleEdit(deck) {
    if (!isAuthenticated) {
      setMessage('Entre com Google antes de editar decks.')
      return
    }
    setMessage(null)
    setEditingDeck(deck)
    setView('edit')
  }

  async function handleDelete(deck) {
    if (!isAuthenticated) {
      setMessage('Entre com Google antes de excluir decks.')
      return
    }
    if (!confirm(`Excluir o deck ${deck.name}?`)) return
    try {
      await deleteDeck(deck.id)
      setMessage(`${deck.name} excluido.`)
      await load()
    } catch (e) {
      console.error('delete failed', e)
      setMessage('Nao foi possivel excluir. Tente novamente.')
    }
  }

  function handleDone(nextMessage, nextDeck = null) {
    if (nextDeck) {
      setEditingDeck(nextDeck)
      setView('edit')
    } else {
      setView('home')
    }
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
          <h1>Biblioteca de Decks</h1>
          <p className="page-description">Crie ou importe um deck, ajuste a lista, valide a legalidade, analise a estrutura e gere recomendacoes explicaveis.</p>
        </div>
        <div className="actions-row">
          <Button className="cta-primary" onClick={handleCreate}>
            <img className="btn-icon" src={createIcon} alt="" aria-hidden="true" />
            Criar Deck
          </Button>
          <Button variant="secondary" onClick={handleImport}>
            <img className="btn-icon" src={importIcon} alt="" aria-hidden="true" />
            Importar Deck
          </Button>
        </div>
      </section>

      <Card className="zone zone-battlefield">
        <div className="workflow-steps" aria-label="Main workflow">
          <div data-state="active"><strong>1</strong><span>Criar ou importar</span></div>
          <div><strong>2</strong><span>Editar lista</span></div>
          <div><strong>3</strong><span>Validar e analisar</span></div>
          <div><strong>4</strong><span>Evoluir o deck</span></div>
        </div>
      </Card>

      {message && <div className="status success">{message}</div>}
      {!isAuthenticated && (
        <div className="status">
          Entre com Google para criar, importar, editar, listar ou excluir seus decks.
        </div>
      )}
      {apiStatus === 'starting' && (
        <StateMessage tone="neutral" title="API iniciando">
          O servidor gratuito pode levar cerca de 50 segundos para acordar apos inatividade. Mantivemos a tela estavel; tente novamente em alguns instantes.
        </StateMessage>
      )}
      {apiStatus === 'unavailable' && (
        <StateMessage tone="error" title="API indisponivel">
          Nao foi possivel conectar com o backend agora. Aguarde alguns segundos e tente recarregar a biblioteca.
        </StateMessage>
      )}
      {loading ? (
        <Card><div className="loading">Carregando decks e aguardando a API responder...</div></Card>
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
