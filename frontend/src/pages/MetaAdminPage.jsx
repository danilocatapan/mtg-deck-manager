import { useCallback, useEffect, useMemo, useState } from 'react'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import StateMessage from '../components/ui/StateMessage'
import {
  getMetaSources,
  getNextRecommendationBenchmarkReview,
  getRecommendationBenchmarkSummary,
  runRecommendationBenchmark,
  submitRecommendationBenchmarkReview,
  syncMeta,
} from '../services/api'
import { getAuthProfile, isMetaAdmin, subscribeAuth } from '../services/auth'
import { diagnosticEvents, diagnosticsEnabled, emitDiagnostic, setDiagnosticsEnabled, subscribeDiagnostics } from '../services/diagnostics'

export default function MetaAdminPage({ onBack }) {
  const [profile, setProfile] = useState(() => getAuthProfile())
  const [sources, setSources] = useState([])
  const [benchmark, setBenchmark] = useState(null)
  const [lastSync, setLastSync] = useState(null)
  const [loading, setLoading] = useState(false)
  const [syncing, setSyncing] = useState(false)
  const [runningBenchmark, setRunningBenchmark] = useState(false)
  const [review, setReview] = useState(null)
  const [reviewing, setReviewing] = useState(false)
  const [diagnostics, setDiagnostics] = useState(() => diagnosticsEnabled())
  const [events, setEvents] = useState(() => diagnosticEvents())
  const [error, setError] = useState(null)
  const canAdmin = isMetaAdmin(profile)

  useEffect(() => subscribeAuth(() => setProfile(getAuthProfile())), [])
  useEffect(() => subscribeDiagnostics(() => {
    setDiagnostics(diagnosticsEnabled())
    setEvents(diagnosticEvents())
  }), [])

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

  async function handleRunBenchmark() {
    setRunningBenchmark(true)
    setError(null)
    try {
      const result = await runRecommendationBenchmark()
      setBenchmark(result)
      emitDiagnostic('event=benchmark.run.completed', { runId: result?.lastRunId, status: result?.status, count: result?.evaluatedCases })
      setReview(await getNextRecommendationBenchmarkReview())
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Nao foi possivel executar o benchmark.'))
      emitDiagnostic('event=benchmark.run.failed', { status: 'failed' })
    } finally {
      setRunningBenchmark(false)
    }
  }

  async function loadNextReview() {
    setReviewing(true)
    try {
      setReview(await getNextRecommendationBenchmarkReview())
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Nao foi possivel carregar a revisao cega.'))
    } finally {
      setReviewing(false)
    }
  }

  async function handleReview(choice) {
    if (!review) return
    setReviewing(true)
    try {
      await submitRecommendationBenchmarkReview(review.caseId, { runId: review.runId, choice })
      emitDiagnostic('event=benchmark.review.recorded', { runId: review.runId, status: choice })
      const [next, summary] = await Promise.all([getNextRecommendationBenchmarkReview(), getRecommendationBenchmarkSummary()])
      setReview(next)
      setBenchmark(summary)
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Nao foi possivel registrar a revisao.'))
    } finally {
      setReviewing(false)
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
          <Button variant="secondary" onClick={handleRunBenchmark} loading={runningBenchmark} loadingLabel="Executando benchmark...">Executar benchmark</Button>
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

      <Card className="zone zone-library">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Benchmark calculado</p>
            <h2>Metricas e amostras</h2>
            <p>Ultima rodada: {benchmark?.lastRunId ? `#${benchmark.lastRunId}` : 'ainda nao executada'} | Casos avaliados: {benchmark?.evaluatedCases || 0}/{benchmark?.totalCases || 20}</p>
          </div>
        </div>
        <div className="meta-admin-info-grid">
          {(benchmark?.metrics || []).map((metric) => (
            <article key={metric.name} className="card action-card benchmark-metric-card">
              <span className={`status-pill ${metric.status === 'ready' ? 'ready' : ''}`}>{metric.status}</span>
              <h3>{metric.name}</h3>
              <p>{metric.value}</p>
              <small>Amostra: {metric.sampleSize || 0} | Meta: {metric.target}</small>
            </article>
          ))}
        </div>
      </Card>

      <Card className="zone zone-battlefield">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Avaliacao humana</p>
            <h2>Revisao cega A/B</h2>
            <p>Progresso: {benchmark?.reviewProgress?.completedCases || 0}/{benchmark?.reviewProgress?.totalCases || benchmark?.totalCases || 20} casos com quorum.</p>
          </div>
          <Button variant="secondary" onClick={loadNextReview} loading={reviewing}>Carregar proximo caso</Button>
        </div>
        {!review ? (
          <div className="empty-inline">Execute o benchmark ou carregue o proximo caso disponivel.</div>
        ) : (
          <>
            <p><strong>{review.commander}</strong> | {review.bracket} | Votos: {review.reviewsCompleted}/{review.reviewsRequired}</p>
            <div className="meta-admin-info-grid">
              <BlindOption title="Opcao A" items={review.optionA} />
              <BlindOption title="Opcao B" items={review.optionB} />
            </div>
            <div className="actions-row">
              <Button loading={reviewing} onClick={() => handleReview('A')}>Prefiro A</Button>
              <Button loading={reviewing} onClick={() => handleReview('B')}>Prefiro B</Button>
              <Button variant="secondary" loading={reviewing} onClick={() => handleReview('tie')}>Empate</Button>
            </div>
          </>
        )}
      </Card>

      <Card className="zone zone-planning">
        <h2>Feedback agregado</h2>
        <p>Util: {benchmark?.feedback?.accepted || 0} | Nao util: {benchmark?.feedback?.rejected || 0} | Precisa revisao: {benchmark?.feedback?.needsReview || 0}</p>
        <p>Comandantes com feedback: {Object.keys(benchmark?.feedbackBreakdown?.byCommander || {}).length} | Brackets: {Object.keys(benchmark?.feedbackBreakdown?.byBracket || {}).length}</p>
      </Card>

      <Card className="zone zone-library">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Observabilidade</p>
            <h2>Diagnostico sanitizado da sessao</h2>
            <p>Nao registra token, identidade, notas privadas ou decklists completas.</p>
          </div>
          <Button variant="secondary" onClick={() => {
            const enabled = !diagnostics
            setDiagnosticsEnabled(enabled)
            if (enabled) emitDiagnostic('event=meta_admin.diagnostics.enabled', { status: 'enabled' })
          }}>{diagnostics ? 'Desativar diagnostico' : 'Ativar diagnostico'}</Button>
        </div>
        {diagnostics && <ul className="quality-limitations" aria-label="Eventos de diagnostico">{events.slice(-10).map((event, index) => <li key={`${event.at}-${index}`}>{event.event} | {event.status || 'ok'} | {event.count ?? '-'}</li>)}</ul>}
      </Card>
    </section>
  )
}

function BlindOption({ title, items = [] }) {
  return (
    <article className="card action-card">
      <h3>{title}</h3>
      <ul className="quality-limitations">
        {items.map((item, index) => <li key={`${item.add}-${index}`}><strong>{item.add}</strong> por {item.remove}: {item.reasoning}</li>)}
      </ul>
    </article>
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
