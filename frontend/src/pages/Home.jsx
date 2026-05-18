import { useCallback, useEffect, useRef, useState } from 'react'
import { consultDeck, deleteDeck, fetchDecks, fetchPublicDecks } from '../services/api'
import DeckList from '../components/DeckList'
import DeckEditorPage from './DeckEditorPage'
import DeckConsultPage from './DeckConsultPage'
import ImportDeckPage from './ImportDeckPage'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import StateMessage from '../components/ui/StateMessage'
import { getAuthToken, subscribeAuth } from '../services/auth'
import { ApiStartingError } from '../services/api'
import createIcon from '../assets/icons/create.png'
import importIcon from '../assets/icons/import.png'

const PUBLIC_DECK_LIMIT = 10
const PUBLIC_COMMANDER_FILTER_DEBOUNCE_MS = 350

export default function Home() {
  const [decks, setDecks] = useState([])
  const [publicDecks, setPublicDecks] = useState([])
  const [publicDecksLoading, setPublicDecksLoading] = useState(true)
  const [publicCommanderFilter, setPublicCommanderFilter] = useState('')
  const [debouncedPublicCommanderFilter, setDebouncedPublicCommanderFilter] = useState('')
  const [view, setView] = useState('home')
  const [editingDeck, setEditingDeck] = useState(null)
  const [consultingDeck, setConsultingDeck] = useState(null)
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(getAuthToken()))
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState(null)
  const [apiStatus, setApiStatus] = useState(null)
  const initialLoadComplete = useRef(false)
  const debouncedPublicCommanderFilterRef = useRef('')

  const loadPublicDecks = useCallback(async function loadPublicDecks(commanderFilter = '') {
    setPublicDecksLoading(true)
    try {
      const loadedPublicDecks = await fetchPublicDecks({
        page: 0,
        size: PUBLIC_DECK_LIMIT,
        commander: commanderFilter.trim(),
        throwOnError: true,
      })
      setApiStatus(null)
      setPublicDecks(loadedPublicDecks)
    } catch (error) {
      console.error('load public decks failed')
      setApiStatus(error instanceof ApiStartingError ? 'starting' : 'unavailable')
      setPublicDecks([])
    } finally {
      setPublicDecksLoading(false)
    }
  }, [])

  const loadUserDecks = useCallback(async function loadUserDecks() {
    const token = getAuthToken()
    try {
      const loadedDecks = token ? await fetchDecks({ throwOnError: true }) : []
      setDecks(loadedDecks)
    } catch (error) {
      console.error('load decks failed')
      setApiStatus(error instanceof ApiStartingError ? 'starting' : 'unavailable')
      setDecks([])
    }
  }, [])

  const load = useCallback(async function loadDecks() {
    setLoading(true)
    setApiStatus(null)
    await Promise.all([
      loadPublicDecks(debouncedPublicCommanderFilter),
      loadUserDecks(),
    ])
    initialLoadComplete.current = true
    setLoading(false)
  }, [debouncedPublicCommanderFilter, loadPublicDecks, loadUserDecks])

  useEffect(() => {
    let cancelled = false
    initialLoadComplete.current = false
    async function loadInitialDecks() {
      setLoading(true)
      setApiStatus(null)
      await Promise.all([
        loadPublicDecks(debouncedPublicCommanderFilterRef.current),
        loadUserDecks(),
      ])
      if (!cancelled) {
        initialLoadComplete.current = true
        setLoading(false)
      }
    }
    queueMicrotask(loadInitialDecks)
    return () => {
      cancelled = true
    }
  }, [isAuthenticated, loadPublicDecks, loadUserDecks])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedPublicCommanderFilter(publicCommanderFilter.trim())
    }, PUBLIC_COMMANDER_FILTER_DEBOUNCE_MS)
    return () => window.clearTimeout(timeoutId)
  }, [publicCommanderFilter])

  useEffect(() => {
    debouncedPublicCommanderFilterRef.current = debouncedPublicCommanderFilter
  }, [debouncedPublicCommanderFilter])

  useEffect(() => {
    if (!initialLoadComplete.current) return
    queueMicrotask(() => loadPublicDecks(debouncedPublicCommanderFilter))
  }, [debouncedPublicCommanderFilter, loadPublicDecks])

  useEffect(() => subscribeAuth(() => {
    setIsAuthenticated(Boolean(getAuthToken()))
  }), [])

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

  async function handleConsult(deck) {
    try {
      setMessage(null)
      const loadedDeck = await consultDeck(deck.id)
      setConsultingDeck(loadedDeck)
      setView('consult')
    } catch {
      console.error('consult deck failed')
      setMessage('Nao foi possivel consultar este deck.')
    }
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
    } catch {
      console.error('delete failed')
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

  function focusLogin() {
    document.getElementById('auth-section')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  function clearPublicCommanderFilter() {
    setPublicCommanderFilter('')
    setDebouncedPublicCommanderFilter('')
  }

  if (view === 'create' || view === 'edit') {
    return <DeckEditorPage mode={view === 'create' ? 'create' : 'edit'} deck={editingDeck} onDone={handleDone} />
  }

  if (view === 'import') {
    return <ImportDeckPage onDone={handleDone} />
  }

  if (view === 'consult') {
    return <DeckConsultPage deck={consultingDeck} onBack={() => setView('home')} />
  }

  const activePublicCommanderFilter = debouncedPublicCommanderFilter.trim()
  const publicEmptyTitle = activePublicCommanderFilter ? 'Nenhum deck encontrado' : 'Nenhum deck publico'
  const publicEmptyDescription = activePublicCommanderFilter
    ? `Nenhum deck publico recente encontrado para "${activePublicCommanderFilter}".`
    : 'Quando um deck for marcado como publico, ele aparecera aqui para consulta.'

  return (
    <main>
      <section className="zone zone-command page-heading">
        <div>
          <p className="eyebrow">Command Zone</p>
          <h1>Biblioteca de Decks</h1>
          <p className="page-description">Consulte decks publicos, crie ou importe sua lista, valide a legalidade, analise a estrutura e gere recomendacoes explicaveis.</p>
        </div>
        <div className="actions-row" aria-describedby={!isAuthenticated ? 'auth-required-message' : undefined}>
          <Button className="cta-primary" onClick={handleCreate} disabled={!isAuthenticated}>
            <img className="btn-icon" src={createIcon} alt="" aria-hidden="true" />
            Criar Deck
          </Button>
          <Button variant="secondary" onClick={handleImport} disabled={!isAuthenticated}>
            <img className="btn-icon" src={importIcon} alt="" aria-hidden="true" />
            Importar Deck
          </Button>
        </div>
      </section>

      <Card className="zone zone-battlefield">
        <div className="workflow-steps" aria-label="Main workflow">
          <div data-state="active"><strong>1</strong><span>Consultar publicos</span></div>
          <div><strong>2</strong><span>Criar ou importar</span></div>
          <div><strong>3</strong><span>Validar e analisar</span></div>
          <div><strong>4</strong><span>Evoluir o deck</span></div>
        </div>
      </Card>

      {message && <div className="status success">{message}</div>}
      {!isAuthenticated && (
        <div id="auth-required-message" className="status auth-callout" role="status" aria-live="polite">
          <div>
            <strong>Login necessario</strong>
            <span>Entre com Google para criar, importar, editar ou excluir seus decks.</span>
          </div>
          <Button variant="secondary" onClick={focusLogin}>Ir para login</Button>
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
        <>
          <Card className="zone zone-library">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Vitrine</p>
                <h2>Decks publicos recentes</h2>
                <p>Top 10 decks publicos criados por ultimo, em modo somente leitura.</p>
              </div>
            </div>
            <div className="public-vitrine-toolbar">
              <label>
                <span>Filtrar por comandante</span>
                <input
                  value={publicCommanderFilter}
                  onChange={(event) => setPublicCommanderFilter(event.target.value)}
                  placeholder="Atraxa, Xenagos, Muldrotha..."
                />
              </label>
              {publicCommanderFilter.trim() && (
                <Button type="button" variant="secondary" onClick={clearPublicCommanderFilter}>
                  Limpar
                </Button>
              )}
              <div className="public-vitrine-summary" aria-live="polite">
                {publicDecksLoading ? 'Atualizando vitrine...' : `${publicDecks.length}/${PUBLIC_DECK_LIMIT} decks recentes`}
              </div>
            </div>
            <DeckList
              decks={publicDecks}
              onConsult={handleConsult}
              showCreateActions={false}
              showManageActions={false}
              emptyTitle={publicEmptyTitle}
              emptyDescription={publicEmptyDescription}
            />
          </Card>

          {isAuthenticated && (
            <Card className="zone zone-library">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">Minha biblioteca</p>
                  <h2>Meus decks</h2>
                  <p>Seus decks continuam privados por padrao e podem ser editados ou excluidos por aqui.</p>
                </div>
              </div>
              <DeckList
                decks={decks}
                onEdit={handleEdit}
                onDelete={handleDelete}
                onCreate={handleCreate}
                onImport={handleImport}
                actionsDisabled={!isAuthenticated}
                actionHint="Entre com Google antes de criar ou importar decks."
              />
            </Card>
          )}
        </>
      )}
    </main>
  )
}
