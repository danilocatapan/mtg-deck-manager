import { useCallback, useEffect, useMemo, useState } from 'react'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import StateMessage from '../components/ui/StateMessage'
import { fetchMetaTopDecks, getMetaTopDeck, importMetaTopDecks, syncMetaTopDecks } from '../services/api'
import { getAuthProfile, isMetaAdmin, subscribeAuth } from '../services/auth'

const DEFAULT_FILTERS = {
  commander: '',
  source: '',
  rankingDate: '',
  bracket: '',
  archetype: '',
  limit: '10',
}

const SAMPLE_IMPORT = `{
  "source": "MOXFIELD",
  "sourceUrl": "https://www.moxfield.com",
  "rankingPeriod": "MONTHLY",
  "rankingDate": "2026-05-01",
  "format": "COMMANDER",
  "decks": [
    {
      "rank": 1,
      "name": "Top Talion Control",
      "commander": "Talion, the Kindly Lord",
      "deckUrl": "https://www.moxfield.com/decks/example",
      "archetype": "CONTROL",
      "bracket": "BRACKET_5",
      "colorIdentity": ["U", "B"],
      "popularityScore": 97.5,
      "cards": [
        { "name": "Talion, the Kindly Lord", "quantity": 1, "section": "COMMANDER" },
        { "name": "Thassa's Oracle", "quantity": 1, "section": "MAIN" },
        { "name": "Demonic Consultation", "quantity": 1, "section": "MAIN" }
      ]
    }
  ]
}`

export default function MetaTopDeckAdminPage({ onBack }) {
  const [profile, setProfile] = useState(() => getAuthProfile())
  const [filters, setFilters] = useState(DEFAULT_FILTERS)
  const [appliedFilters, setAppliedFilters] = useState(DEFAULT_FILTERS)
  const [decks, setDecks] = useState([])
  const [selectedDeck, setSelectedDeck] = useState(null)
  const [payload, setPayload] = useState(SAMPLE_IMPORT)
  const [loadingList, setLoadingList] = useState(false)
  const [loadingDetailId, setLoadingDetailId] = useState(null)
  const [importing, setImporting] = useState(false)
  const [syncing, setSyncing] = useState(false)
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)
  const [warnings, setWarnings] = useState([])
  const canAdmin = isMetaAdmin(profile)

  useEffect(() => subscribeAuth(() => setProfile(getAuthProfile())), [])

  const loadDecks = useCallback(async () => {
    if (!canAdmin) return
    setLoadingList(true)
    setError(null)
    try {
      const loadedDecks = await fetchMetaTopDecks(appliedFilters)
      setDecks(Array.isArray(loadedDecks) ? loadedDecks : [])
    } catch (exception) {
      setDecks([])
      setError(adminErrorMessage(exception, 'Nao foi possivel listar os top decks.'))
    } finally {
      setLoadingList(false)
    }
  }, [appliedFilters, canAdmin])

  useEffect(() => {
    queueMicrotask(loadDecks)
  }, [loadDecks])

  const deckRows = useMemo(() => decks.slice(0, Number(appliedFilters.limit || 10)), [appliedFilters.limit, decks])

  function updateFilter(field, value) {
    setFilters((current) => ({ ...current, [field]: value }))
  }

  async function handleSubmitFilters(event) {
    event.preventDefault()
    setSelectedDeck(null)
    setAppliedFilters(filters)
  }

  async function handleSelectDeck(deck) {
    setLoadingDetailId(deck.id)
    setError(null)
    try {
      const detail = await getMetaTopDeck(deck.id)
      setSelectedDeck(detail)
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Nao foi possivel abrir o detalhe do top deck.'))
    } finally {
      setLoadingDetailId(null)
    }
  }

  async function handleImport(event) {
    event.preventDefault()
    setImporting(true)
    setError(null)
    setMessage(null)
    setWarnings([])

    try {
      const parsedPayload = JSON.parse(payload)
      const response = await importMetaTopDecks(parsedPayload)
      setMessage(importMessage(response))
      setWarnings(response?.warnings || [])
      await loadDecks()
    } catch (exception) {
      setError(exception instanceof SyntaxError
        ? 'JSON invalido. Revise virgulas, aspas e chaves antes de importar.'
        : adminErrorMessage(exception, 'Falha na importacao dos top decks.'))
    } finally {
      setImporting(false)
    }
  }

  async function handleSync() {
    setSyncing(true)
    setError(null)
    setMessage(null)
    try {
      const response = await syncMetaTopDecks({
        source: filters.source || 'MANUAL',
        rankingPeriod: 'MONTHLY',
        rankingDate: filters.rankingDate || currentMonthDate(),
        limitPerGroup: 3,
        groupBy: 'COMMANDER',
      })
      setMessage(response?.message || 'Sincronizacao registrada com sucesso.')
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Nao foi possivel registrar a sincronizacao.'))
    } finally {
      setSyncing(false)
    }
  }

  if (!canAdmin) {
    return (
      <main>
        <section className="zone zone-command page-heading">
          <div>
            <p className="eyebrow">Meta Admin</p>
            <h1>Acesso restrito</h1>
            <p className="page-description">
              Esta tela importa top decks externos e exige login Google autorizado.
            </p>
          </div>
          <Button variant="secondary" onClick={onBack}>Voltar</Button>
        </section>
        <StateMessage tone="error" title="Permissao necessaria">
          Entre com o usuario Google dcatapan@gmail.com para liberar estes controles.
        </StateMessage>
      </main>
    )
  }

  return (
    <main className="meta-admin-page">
      <section className="zone zone-command page-heading">
        <div>
          <p className="eyebrow">Meta Admin</p>
          <h1>Top decks externos</h1>
          <p className="page-description">
            Use esta tela para importar top decks externos e transformar listas competitivas em sinais de meta para recomendacoes.
          </p>
        </div>
        <div className="actions-row">
          <Button variant="secondary" onClick={handleSync} loading={syncing}>Registrar sync</Button>
          <Button variant="secondary" onClick={onBack}>Voltar</Button>
        </div>
      </section>

      {message && <div className="status success">{message}</div>}
      {error && <div className="status error">{error}</div>}
      {warnings.length > 0 && (
        <div className="status meta-admin-warnings" role="status">
          <strong>Avisos da importacao</strong>
          {warnings.slice(0, 6).map((warning) => <span key={warning}>{warning}</span>)}
          {warnings.length > 6 && <span>Mais {warnings.length - 6} avisos omitidos nesta tela.</span>}
        </div>
      )}

      <div className="meta-admin-info-grid">
        <Card className="zone zone-library meta-admin-info-card">
          <h2>Para que serve</h2>
          <p>Importa rankings externos e guarda snapshots por periodo.</p>
          <p>Reimportar o mesmo ranking atualiza; outro mes cria historico.</p>
          <p>Com 3 decks por comandante + bracket, cartas recorrentes podem influenciar recomendacoes.</p>
        </Card>
        <Card className="zone zone-planning meta-admin-info-card">
          <h2>Como importar</h2>
          <p>Cole um JSON no formato esperado pela API.</p>
          <p>Use MAIN para cartas do deck e COMMANDER para o comandante.</p>
          <p>Deck incompleto entra com aviso; commander invalido rejeita o deck.</p>
        </Card>
        <Card className="zone zone-battlefield meta-admin-info-card">
          <h2>O que melhora</h2>
          <p>Mais sugestoes baseadas em uso real de top decks.</p>
          <p>Recomendacoes podem indicar origem meta_top_decks.</p>
          <p>Menos fallback generico quando houver amostra suficiente.</p>
        </Card>
      </div>

      <div className="split-layout meta-admin-layout">
        <Card className="zone zone-library">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Consulta</p>
              <h2>Decks importados</h2>
              <p>Filtre, abra detalhes e confirme quais listas ja alimentam o meta local.</p>
            </div>
          </div>

          <form className="meta-admin-filters" onSubmit={handleSubmitFilters}>
            <label>
              Commander
              <input value={filters.commander} onChange={(event) => updateFilter('commander', event.target.value)} placeholder="Talion, Yuriko..." />
            </label>
            <label>
              Source
              <select value={filters.source} onChange={(event) => updateFilter('source', event.target.value)}>
                <option value="">Todas</option>
                <option value="MOXFIELD">MOXFIELD</option>
                <option value="TOPDECK_GG">TOPDECK_GG</option>
                <option value="EDHREC">EDHREC</option>
                <option value="MANUAL">MANUAL</option>
              </select>
            </label>
            <label>
              Ranking date
              <input type="date" value={filters.rankingDate} onChange={(event) => updateFilter('rankingDate', event.target.value)} />
            </label>
            <label>
              Bracket
              <select value={filters.bracket} onChange={(event) => updateFilter('bracket', event.target.value)}>
                <option value="">Todos</option>
                <option value="BRACKET_3">BRACKET_3</option>
                <option value="BRACKET_4">BRACKET_4</option>
                <option value="BRACKET_5">BRACKET_5</option>
                <option value="UNKNOWN">UNKNOWN</option>
              </select>
            </label>
            <label>
              Archetype
              <select value={filters.archetype} onChange={(event) => updateFilter('archetype', event.target.value)}>
                <option value="">Todos</option>
                <option value="AGGRO">AGGRO</option>
                <option value="CONTROL">CONTROL</option>
                <option value="COMBO">COMBO</option>
                <option value="STAX">STAX</option>
                <option value="MIDRANGE">MIDRANGE</option>
                <option value="UNKNOWN">UNKNOWN</option>
              </select>
            </label>
            <label>
              Limit
              <input type="number" min="1" max="50" value={filters.limit} onChange={(event) => updateFilter('limit', event.target.value)} />
            </label>
            <div className="form-actions meta-admin-filter-actions">
              <Button type="submit" loading={loadingList}>Atualizar lista</Button>
            </div>
          </form>

          {loadingList ? (
            <div className="empty-inline">Carregando top decks...</div>
          ) : deckRows.length === 0 ? (
            <div className="empty-inline">Nenhum top deck encontrado para os filtros atuais.</div>
          ) : (
            <div className="meta-top-deck-list">
              {deckRows.map((deck) => (
                <button key={deck.id} type="button" className="meta-top-deck-row" onClick={() => handleSelectDeck(deck)}>
                  <strong>#{deck.rank} {deck.name}</strong>
                  <span>{deck.commander}</span>
                  <small>{deck.source} - {deck.rankingDate} - {deck.bracket} - {deck.cardsCount} cartas</small>
                  <em>{loadingDetailId === deck.id ? 'Abrindo...' : 'Ver detalhes'}</em>
                </button>
              ))}
            </div>
          )}
        </Card>

        <Card className="zone zone-planning">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Detalhe</p>
              <h2>{selectedDeck?.name || 'Selecione um deck'}</h2>
              <p>Revise rank, bracket, origem e cartas persistidas.</p>
            </div>
          </div>

          {selectedDeck ? (
            <>
              <div className="metric-grid meta-admin-detail-metrics">
                <Metric label="Rank" value={`#${selectedDeck.rank}`} />
                <Metric label="Commander" value={selectedDeck.commander} />
                <Metric label="Bracket" value={selectedDeck.bracket} />
                <Metric label="Archetype" value={selectedDeck.archetype} />
                <Metric label="Source" value={selectedDeck.source} />
                <Metric label="Periodo" value={selectedDeck.rankingDate} />
              </div>
              <div className="meta-admin-card-table">
                {(selectedDeck.cards || []).map((card) => (
                  <div key={`${card.section}-${card.name}`} className="deck-row">
                    <strong>{card.name}</strong>
                    <span>{card.quantity}x {card.section}{printingLabel(card) ? ` - ${printingLabel(card)}` : ''}</span>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="empty-inline">Abra um item da lista para ver as cartas importadas.</div>
          )}
        </Card>
      </div>

      <Card className="zone zone-library">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Importacao</p>
            <h2>Importar JSON</h2>
            <p>O backend valida commander, secoes permitidas, duplicatas e identidade de cor antes de alimentar os sinais de meta.</p>
          </div>
        </div>
        <form onSubmit={handleImport}>
          <label className="meta-admin-payload-label">
            Payload estruturado
            <textarea rows={18} value={payload} onChange={(event) => setPayload(event.target.value)} spellCheck="false" />
          </label>
          <div className="form-actions">
            <Button type="submit" loading={importing}>Importar top decks</Button>
            <Button type="button" variant="secondary" onClick={() => setPayload(SAMPLE_IMPORT)}>Usar exemplo</Button>
          </div>
        </form>
      </Card>
    </main>
  )
}

function Metric({ label, value }) {
  return (
    <div className="metric-card">
      <span>{label}</span>
      <strong>{value || '-'}</strong>
    </div>
  )
}

function adminErrorMessage(exception, fallback) {
  const message = exception?.message || ''
  if (message.includes('Forbidden') || message.includes('403')) {
    return 'Acesso negado. Entre com o Google autorizado para usar o Meta Admin.'
  }
  return message || fallback
}

function importMessage(response) {
  if (!response) return 'Importacao concluida.'
  return `Importacao ${response.status}: ${response.importedDecks} importados, ${response.updatedDecks} atualizados, ${response.ignoredDecks} ignorados.`
}

function printingLabel(card) {
  const edition = card?.setCode ? `${card.setCode}${card.collectorNumber ? ` #${card.collectorNumber}` : ''}` : ''
  const finish = card?.finish && card.finish !== 'UNKNOWN' ? String(card.finish).toLowerCase() : ''
  return [edition, finish].filter(Boolean).join(' - ')
}

function currentMonthDate() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
}
