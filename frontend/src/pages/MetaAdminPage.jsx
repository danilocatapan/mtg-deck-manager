import { useCallback, useEffect, useMemo, useState } from 'react'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import StateMessage from '../components/ui/StateMessage'
import { getMetaSources, getRecommendationBenchmarkSummary, syncMeta } from '../services/api'
import { getAuthProfile, isMetaAdmin, subscribeAuth } from '../services/auth'

export default function MetaAdminPage({ onBack }) {
  const [profile, setProfile] = useState(() => getAuthProfile())
  const [sources, setSources] = useState([])
  const [benchmark, setBenchmark] = useState(null)
  const [lastSync, setLastSync] = useState(null)
  const [loading, setLoading] = useState(false)
  const [syncing, setSyncing] = useState(false)
  const [error, setError] = useState(null)
  const canAdmin = isMetaAdmin(profile)

  useEffect(() => subscribeAuth(() => setProfile(getAuthProfile())), [])

  const loadStatus = useCallback(async () => {
    if (!canAdmin) return
    setLoading(true)
    setError(null)
    try {
      const [loadedSources, loadedBenchmark] = await Promise.all([
        getMetaSources(),
        getRecommendationBenchmarkSummary(),
      ])
      setSources(Array.isArray(loadedSources) ? loadedSources : [])
      setBenchmark(loadedBenchmark)
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Nao foi possivel carregar o estado do meta.'))
    } finally {
      setLoading(false)
    }
  }, [canAdmin])

  useEffect(() => {
    queueMicrotask(loadStatus)
  }, [loadStatus])

  async function handleSync() {
    setSyncing(true)
    setError(null)
    try {
      const result = await syncMeta()
      setLastSync(result)
      await loadStatus()
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Nao foi possivel sincronizar o meta.'))
    } finally {
      setSyncing(false)
    }
  }

  const topDeck = useMemo(
    () => sources.find((source) => String(source?.name || '').toLowerCase().includes('topdeck')),
    [sources],
  )
  const coverage = lastSync?.coverageByBracket || {}
  const actions = Array.isArray(benchmark?.nextActions) ? benchmark.nextActions : []

  if (!canAdmin) {
    return (
      <section className="meta-admin-page">
        <section className="zone zone-command page-heading">
          <div>
            <p className="eyebrow">Meta Admin</p>
            <h1>Acesso restrito</h1>
            <p className="page-description">Este painel acompanha e sincroniza automaticamente a base de meta.</p>
          </div>
          <Button variant="secondary" onClick={onBack}>Voltar</Button>
        </section>
        <StateMessage tone="error" title="Permissao necessaria">
          Entre com um usuario Google autorizado para liberar estes controles.
        </StateMessage>
      </section>
    )
  }

  return (
    <section className="meta-admin-page">
      <section className="zone zone-command page-heading">
        <div>
          <p className="eyebrow">Meta Admin</p>
          <h1>Meta automatico</h1>
          <p className="page-description">
            TopDeck.gg e a unica fonte externa viva. A sincronizacao busca, valida, persiste e recalcula os perfis sem importar decklists manualmente.
          </p>
        </div>
        <div className="actions-row">
          <Button onClick={handleSync} loading={syncing} loadingLabel="Sincronizando meta...">Sincronizar meta agora</Button>
          <Button variant="secondary" onClick={onBack}>Voltar</Button>
        </div>
      </section>

      {loading && <StateMessage title="Carregando estado operacional...">Consultando fonte e benchmark.</StateMessage>}
      {error && <StateMessage tone="error" title="Falha no painel">{error}</StateMessage>}
      {lastSync && (
        <StateMessage tone={lastSync.status === 'success' ? 'success' : 'warning'} title="Sincronizacao concluida">
          {lastSync.status === 'success'
            ? `${lastSync.importedDecks} decks persistidos e ${lastSync.profilesBuilt} perfis reconstruidos.`
            : `Nenhum snapshot novo foi recebido; ${lastSync.snapshotDecks || 0} decks do ultimo snapshot valido foram preservados.`}
          {lastSync.errors?.length > 0 && ` Erros: ${lastSync.errors.join(', ')}.`}
        </StateMessage>
      )}

      <div className="meta-admin-info-grid">
        <Card className="zone zone-library meta-admin-info-card">
          <h2>Fonte viva</h2>
          <p><strong>TopDeck.gg</strong></p>
          <p>Configurada: {topDeck?.enabled ? 'sim' : 'nao'}</p>
          <p>Ultimo sync: {formatDate(topDeck?.lastSync)}</p>
        </Card>
        <Card className="zone zone-planning meta-admin-info-card">
          <h2>Cobertura atual</h2>
          <p>Decks: {lastSync?.snapshotDecks ?? 'sincronize para consultar'}</p>
          <p>Comandantes: {lastSync?.commandersCovered ?? '-'}</p>
          <p>{Object.entries(coverage).map(([bracket, count]) => `${bracket}: ${count}`).join(' | ') || 'Cobertura por bracket ainda nao carregada.'}</p>
        </Card>
        <Card className="zone zone-battlefield meta-admin-info-card">
          <h2>Fallback</h2>
          <p>O dataset local embarcado cobre casual/mid quando TopDeck.gg nao possui amostra suficiente.</p>
          <p>Nenhuma decklist precisa ser colada ou importada pelo administrador.</p>
        </Card>
      </div>

      <Card className="zone zone-planning">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Progresso derivado</p>
            <h2>Proximas acoes da melhoria</h2>
            <p>Estas tarefas sao calculadas pelo estado real do benchmark e do feedback.</p>
          </div>
        </div>
        <div className="meta-admin-action-list">
          {actions.length === 0 ? (
            <div className="empty-inline">Sem acoes carregadas.</div>
          ) : actions.map((action) => (
            <article key={action.id} className="card action-card">
              <span className={`status-pill ${action.status === 'ready' ? 'ready' : action.status === 'blocked' ? 'danger' : ''}`}>
                {action.status}
              </span>
              <h3>{action.title}</h3>
              <p>{action.description}</p>
              <small>Responsavel: {actorLabel(action.actor)}{action.target ? ` | Progresso: ${action.completed || 0}/${action.target}` : ''}</small>
            </article>
          ))}
        </div>
      </Card>
    </section>
  )
}

function formatDate(value) {
  return value ? String(value).slice(0, 19).replace('T', ' ') : 'nunca'
}

function actorLabel(actor) {
  if (actor === 'user') return 'usuario'
  if (actor === 'reviewer') return 'avaliador'
  return 'mantenedor'
}

function adminErrorMessage(exception, fallback) {
  const message = exception?.message || ''
  if (message.includes('Forbidden') || message.includes('403')) {
    return 'Acesso negado. Entre com o Google autorizado para usar o Meta Admin.'
  }
  return message || fallback
}
