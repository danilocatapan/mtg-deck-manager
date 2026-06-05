import { useCallback, useEffect, useMemo, useState } from 'react'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import StateMessage from '../components/ui/StateMessage'
import {
  getMetaSources,
  getNextRecommendationBenchmarkReview,
  generateRecommendationBenchmarkAi,
  getRecommendationBenchmarkAiJob,
  getRecommendationBenchmarkComparison,
  getRecommendationBenchmarkSummary,
  previewRecommendationBenchmarkAi,
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
  const [aiPreview, setAiPreview] = useState(null)
  const [aiJob, setAiJob] = useState(null)
  const [generatingAi, setGeneratingAi] = useState(false)
  const [review, setReview] = useState(null)
  const [reviewing, setReviewing] = useState(false)
  const [comparison, setComparison] = useState(null)
  const [comparisonLoading, setComparisonLoading] = useState(false)
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
      setAiJob(loadedBenchmark?.aiArtifacts?.latestJob || null)
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Não foi possível carregar o estado do meta.'))
    } finally {
      setLoading(false)
    }
  }, [canAdmin])

  useEffect(() => {
    if (!aiJob?.id || aiJob.status !== 'running') return undefined
    const timer = window.setInterval(async () => {
      try {
        const current = await getRecommendationBenchmarkAiJob(aiJob.id)
        setAiJob(current)
        if (current.status !== 'running') await loadStatus()
      } catch {
        window.clearInterval(timer)
      }
    }, 2000)
    return () => window.clearInterval(timer)
  }, [aiJob?.id, aiJob?.status, loadStatus])

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
      setError(adminErrorMessage(exception, 'Não foi possível sincronizar o meta.'))
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
      await loadStatus()
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Não foi possível executar o benchmark.'))
      emitDiagnostic('event=benchmark.run.failed', { status: 'failed' })
    } finally {
      setRunningBenchmark(false)
    }
  }

  async function handlePreviewAi() {
    setError(null)
    try {
      setAiPreview(await previewRecommendationBenchmarkAi())
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Não foi possível calcular a previsão da geração GPT.'))
    }
  }

  async function handleGenerateAi() {
    setGeneratingAi(true)
    setError(null)
    try {
      const job = await generateRecommendationBenchmarkAi()
      setAiJob(job)
      emitDiagnostic('event=benchmark.ai_job.started', { status: job.status, count: job.totalCalls })
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Não foi possível iniciar a geração GPT.'))
      emitDiagnostic('event=benchmark.ai_job.failed', { status: 'failed' })
    } finally {
      setGeneratingAi(false)
    }
  }

  async function loadNextReview() {
    setReviewing(true)
    try {
      setReview(await getNextRecommendationBenchmarkReview())
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Não foi possível carregar a revisão cega.'))
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
      setError(adminErrorMessage(exception, 'Não foi possível registrar a revisão.'))
    } finally {
      setReviewing(false)
    }
  }

  async function handleComparison(caseId) {
    setComparisonLoading(true)
    setError(null)
    try {
      setComparison(await getRecommendationBenchmarkComparison(caseId))
    } catch (exception) {
      setError(adminErrorMessage(exception, 'Não foi possível abrir o diagnóstico deste caso.'))
    } finally {
      setComparisonLoading(false)
    }
  }

  const topDeck = useMemo(
    () => sources.find((source) => String(source?.name || '').toLowerCase().includes('topdeck')),
    [sources],
  )
  const coverage = lastSync?.coverageByBracket || {}
  const actions = Array.isArray(benchmark?.nextActions) ? benchmark.nextActions : []
  const pipeline = Array.isArray(benchmark?.pipeline) ? benchmark.pipeline : []
  const blockers = Array.isArray(benchmark?.corpusStatus?.blockers) ? benchmark.corpusStatus.blockers : []
  const caseSummaries = Array.isArray(benchmark?.aiArtifacts?.caseSummaries) ? benchmark.aiArtifacts.caseSummaries : []
  const baselineMetrics = benchmark?.aiArtifacts?.metrics || {}

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
        <StateMessage tone="error" title="Permissão necessária">
          Entre com um usuário Google autorizado para liberar estes controles.
        </StateMessage>
      </section>
    )
  }

  return (
    <section className="meta-admin-page">
      <section className="zone zone-command page-heading">
        <div>
          <p className="eyebrow">Meta Admin</p>
          <h1>Meta automático</h1>
          <p className="page-description">
            TopDeck.gg é a única fonte externa ativa. A sincronização busca, valida, persiste e recalcula os perfis sem importar listas manualmente.
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
        <StateMessage tone={lastSync.status === 'success' ? 'success' : 'warning'} title="Sincronização concluída">
          {lastSync.status === 'success'
            ? `${lastSync.importedDecks} decks persistidos e ${lastSync.profilesBuilt} perfis reconstruidos.`
            : `Nenhuma captura nova foi recebida; ${lastSync.snapshotDecks || 0} decks da última captura válida foram preservados.`}
          {lastSync.errors?.length > 0 && ` Erros: ${lastSync.errors.join(', ')}.`}
        </StateMessage>
      )}

      <div className="meta-admin-info-grid">
        <Card className="zone zone-library meta-admin-info-card">
          <h2>Fonte viva</h2>
          <p><strong>TopDeck.gg</strong></p>
          <p>Configurada: {topDeck?.enabled ? 'sim' : 'não'}</p>
          <p>Última sincronização: {formatDate(topDeck?.lastSync)}</p>
        </Card>
        <Card className="zone zone-planning meta-admin-info-card">
          <h2>Cobertura atual</h2>
          <p>Decks: {lastSync?.snapshotDecks ?? 'sincronize para consultar'}</p>
          <p>Comandantes: {lastSync?.commandersCovered ?? '-'}</p>
          <p>{Object.entries(coverage).map(([bracket, count]) => `${bracket}: ${count}`).join(' | ') || 'Cobertura por nível ainda não carregada.'}</p>
        </Card>
        <Card className="zone zone-battlefield meta-admin-info-card">
          <h2>Alternativa local</h2>
          <p>O conjunto de dados local cobre casual/mid quando TopDeck.gg não possui amostra suficiente.</p>
          <p>Nenhuma decklist precisa ser colada ou importada pelo administrador.</p>
        </Card>
      </div>

      <Card className="zone zone-planning">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Prova de qualidade</p>
            <h2>Funil operacional do corpus</h2>
            <p>Cada etapa mostra o que está pronto e o que bloqueia uma vantagem automática qualificada.</p>
          </div>
        </div>
        <ol className="benchmark-funnel" aria-label="Funil operacional do benchmark">
          {pipeline.map((stage) => (
            <li key={stage.id} className={stage.status === 'ready' ? 'ready' : ''}>
              <span>{stage.completed ?? 0}/{stage.target ?? '-'}</span>
              <strong>{pipelineLabel(stage.id)}</strong>
              <small>{stage.status === 'ready' ? 'Concluído' : 'Em andamento'}</small>
            </li>
          ))}
        </ol>
        {blockers.length > 0 && (
          <div className="benchmark-blockers">
            <strong>O que precisa acontecer agora</strong>
            <ul className="quality-limitations">{blockers.map((blocker) => <li key={blocker}>{blockerLabel(blocker)}</li>)}</ul>
          </div>
        )}
      </Card>

      <Card className="zone zone-planning">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Progresso derivado</p>
            <h2>Próximas ações da melhoria</h2>
            <p>Estas tarefas são calculadas pelo estado real do benchmark e das avaliações.</p>
          </div>
        </div>
        <div className="meta-admin-action-list">
          {actions.length === 0 ? (
            <div className="empty-inline">Sem ações carregadas.</div>
          ) : actions.map((action) => (
            <article key={action.id} className="card action-card">
              <span className={`status-pill ${action.status === 'ready' ? 'ready' : action.status === 'blocked' ? 'danger' : ''}`}>
                {statusLabel(action.status)}
              </span>
              <h3>{action.title}</h3>
              <p>{action.description}</p>
              <small>Responsável: {actorLabel(action.actor)}{action.target ? ` | Progresso: ${action.completed || 0}/${action.target}` : ''}</small>
            </article>
          ))}
        </div>
      </Card>

      <Card className="zone zone-library">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Benchmark calculado</p>
            <h2>Métricas e amostras</h2>
            <p>Última rodada: {benchmark?.lastRunId ? `#${benchmark.lastRunId}` : 'ainda não executada'} | Casos avaliados: {benchmark?.evaluatedCases || 0}/{benchmark?.totalCases || 20}</p>
          </div>
        </div>
        <div className="meta-admin-info-grid">
          {(benchmark?.metrics || []).map((metric) => (
            <article key={metric.name} className="card action-card benchmark-metric-card">
              <span className={`status-pill ${metric.status === 'ready' ? 'ready' : ''}`}>{statusLabel(metric.status)}</span>
              <h3>{metricLabel(metric.name)}</h3>
              <p>{metric.value}</p>
              <small>Amostra: {metric.sampleSize || 0} | Meta: {metric.target}</small>
            </article>
          ))}
        </div>
      </Card>

      <Card className="zone zone-command">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Comparação automática qualificada</p>
            <h2>Referências e juiz GPT-5.5</h2>
            <p>
              Resultado automático, versionado e sem validação humana concluída. Nunca ajusta pesos automaticamente.
            </p>
          </div>
          <div className="actions-row">
            <Button variant="secondary" onClick={handlePreviewAi}>Visualizar previsão</Button>
            <Button onClick={handleGenerateAi} loading={generatingAi || aiJob?.status === 'running'} loadingLabel="Gerando artefatos GPT...">Gerar comparações GPT</Button>
          </div>
        </div>
        <div className="meta-admin-info-grid">
          <article className="card action-card">
            <span className={`status-pill ${benchmark?.aiArtifacts?.promotedSetCurrent ? 'ready' : ''}`}>
              {benchmark?.aiArtifacts?.promotedSetCurrent ? 'atual' : 'pendente'}
            </span>
            <h3>Conjunto promovido</h3>
            <p>Modelo: {benchmark?.aiArtifacts?.model || 'gpt-5.5'}</p>
            <small>Validação humana: {humanValidationLabel(benchmark?.aiArtifacts?.humanValidation)}</small>
          </article>
          <article className="card action-card">
            <h3>Previsão operacional</h3>
            <p>{aiPreview ? `${aiPreview.totalCalls} chamadas máximas para ${aiPreview.totalCases} casos.` : 'Visualize antes de iniciar para conferir escopo e configuração.'}</p>
            <small>{aiPreview ? `${previewStatusLabel(aiPreview.status)} | Referências: ${aiPreview.baselineCalls} | Juízes: ${aiPreview.judgeCalls} | Concorrência: ${aiPreview.maxConcurrency}` : 'A chave permanece somente no backend.'}</small>
          </article>
          <article className="card action-card">
            <h3>Processamento atual</h3>
            <p>{aiJob ? `${aiJob.completedCalls}/${aiJob.totalCalls} artefatos | ${statusLabel(aiJob.status)}` : 'Nenhum processamento iniciado.'}</p>
            <small>{aiJob?.errorCode ? `Falha: ${aiJob.errorCode}. O último conjunto válido foi preservado.` : 'Processamentos incompletos nunca substituem o último conjunto válido.'}</small>
          </article>
        </div>
        <div className="baseline-results-grid">
          {['generic', 'grounded'].map((kind) => <BaselineResult key={kind} kind={kind} metrics={baselineMetrics[kind]} />)}
        </div>
        <section className="benchmark-case-browser">
          <div className="section-heading compact">
            <div>
              <p className="eyebrow">Diagnósticos sanitizados</p>
              <h3>Casos do conjunto promovido</h3>
            </div>
          </div>
          {caseSummaries.length === 0 ? (
            <div className="empty-inline">Os casos aparecerão aqui quando o primeiro conjunto GPT completo for promovido.</div>
          ) : (
            <div className="benchmark-case-list">
              {caseSummaries.map((item) => (
                <button type="button" key={item.caseId} onClick={() => handleComparison(item.caseId)}>
                  <strong>{item.commander}</strong>
                  <span>Nível: {item.bracket} | Fonte: {item.source}</span>
                  <small>Genérico: {winnerLabel(item.genericWinner)} | Contextualizado: {winnerLabel(item.groundedWinner)} | Problemas: {item.problemCount}</small>
                </button>
              ))}
            </div>
          )}
          {comparisonLoading && <p>Carregando diagnóstico...</p>}
          {comparison && (
            <article className="benchmark-comparison-detail">
              <span className="status-pill">{comparisonStatusLabel(comparison.status)}</span>
              <h3>{comparison.commander}</h3>
              <p>Genérico: {winnerSummary(comparison.generic)} | Contextualizado: {winnerSummary(comparison.grounded)}</p>
              <strong>Melhorias sugeridas</strong>
              <ul className="quality-limitations">{(comparison.suggestedImprovements || []).slice(0, 6).map((item) => <li key={item}>{item}</li>)}</ul>
            </article>
          )}
        </section>
      </Card>

      <Card className="zone zone-battlefield">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Validação futura</p>
            <h2>Revisão humana cega A/B</h2>
            <p>Progresso: {benchmark?.reviewProgress?.completedCases || 0}/{benchmark?.reviewProgress?.totalCases || benchmark?.totalCases || 20} casos com quórum.</p>
          </div>
          <Button variant="secondary" onClick={loadNextReview} loading={reviewing}>Carregar próximo caso</Button>
        </div>
        {!review ? (
          <div className="empty-inline">Execute o benchmark ou carregue o próximo caso disponível.</div>
        ) : (
          <>
            <p><strong>{review.commander}</strong> | {review.bracket} | Votos: {review.reviewsCompleted}/{review.reviewsRequired}</p>
            <div className="meta-admin-info-grid">
              <BlindOption title="Opção A" items={review.optionA} />
              <BlindOption title="Opção B" items={review.optionB} />
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
        <h2>Avaliações agregadas</h2>
        <p>Útil: {benchmark?.feedback?.accepted || 0} | Não útil: {benchmark?.feedback?.rejected || 0} | Precisa de revisão: {benchmark?.feedback?.needsReview || 0}</p>
        <p>Comandantes com avaliações: {Object.keys(benchmark?.feedbackBreakdown?.byCommander || {}).length} | Níveis: {Object.keys(benchmark?.feedbackBreakdown?.byBracket || {}).length}</p>
      </Card>

      <Card className="zone zone-library">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Observabilidade</p>
            <h2>Diagnóstico sanitizado da sessão</h2>
            <p>Não registra token, identidade, notas privadas ou listas completas.</p>
          </div>
          <Button variant="secondary" onClick={() => {
            const enabled = !diagnostics
            setDiagnosticsEnabled(enabled)
            if (enabled) emitDiagnostic('event=meta_admin.diagnostics.enabled', { status: 'enabled' })
          }}>{diagnostics ? 'Desativar diagnóstico' : 'Ativar diagnóstico'}</Button>
        </div>
        {diagnostics && <ul className="quality-limitations" aria-label="Eventos de diagnóstico">{events.slice(-10).map((event, index) => <li key={`${event.at}-${index}`}>{event.event} | {event.status || 'ok'} | {event.count ?? '-'}</li>)}</ul>}
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
  if (actor === 'user') return 'usuário'
  if (actor === 'reviewer') return 'avaliador'
  return 'mantenedor'
}

function adminErrorMessage(exception, fallback) {
  const message = exception?.message || ''
  if (message.includes('Forbidden') || message.includes('403')) {
    return 'Acesso negado. Entre com o Google autorizado para usar o Meta Admin.'
  }
  if (message.includes('corpus_not_ready')) return 'Complete e valide os 50 casos reais antes de gerar comparações GPT.'
  if (message.includes('openai_not_configured') || message.includes('missing_openai_api_key')) return 'Configure a credencial OpenAI somente no backend para iniciar a geração.'
  return message || fallback
}

function BaselineResult({ kind, metrics }) {
  return (
    <article className="baseline-result-card">
      <span>{kind === 'generic' ? 'GPT genérico' : 'GPT contextualizado'}</span>
      <strong>{metrics ? `${Math.round(Number(metrics.systemWinRate || 0) * 100)}% de vitórias do sistema` : 'Aguardando conjunto promovido'}</strong>
      <small>{metrics ? `${metrics.systemWins || 0} sistema | ${metrics.gptWins || 0} GPT | ${metrics.ties || 0} empates` : 'Sem resultado qualificável.'}</small>
    </article>
  )
}

function pipelineLabel(id) {
  return ({
    candidates: 'Candidatos encontrados',
    snapshots: 'Capturas completas',
    valid: 'Casos válidos',
    offline: 'Benchmark local',
    artifacts: 'Artefatos GPT',
    promoted: 'Conjunto promovido',
  })[id] || id
}

function blockerLabel(code) {
  return ({
    archidekt_snapshots_missing: 'Congelar listas e metadados completos dos 25 candidatos Archidekt.',
    topdeck_snapshots_missing: 'Coletar 25 decks competitivos TopDeck.gg com a chave configurada.',
    corpus_not_ready: 'Completar e validar os 50 casos reais antes de gerar comparações GPT.',
    distinct_commanders_required: 'Garantir um comandante diferente em cada caso.',
  })[code] || String(code).replaceAll('_', ' ')
}

function winnerLabel(winner) {
  if (winner === 'system') return 'sistema'
  if (winner === 'gpt') return 'GPT'
  return 'empate'
}

function winnerSummary(metrics = {}) {
  return `${metrics.systemWins || 0} sistema, ${metrics.gptWins || 0} GPT, ${metrics.ties || 0} empates`
}

function previewStatusLabel(status) {
  if (status === 'ready_to_generate') return 'Pronto para gerar'
  if (status === 'corpus_not_ready') return 'Corpus real ainda incompleto'
  if (status === 'missing_openai_api_key') return 'Credencial OpenAI ausente no backend'
  return status || 'Estado indisponível'
}

function statusLabel(status) {
  return ({
    ready: 'Pronto',
    blocked: 'Bloqueado',
    pending: 'Pendente',
    in_progress: 'Em andamento',
    needs_attention: 'Requer atenção',
    not_ready: 'Ainda não pronto',
    running: 'Em execução',
    success: 'Concluído',
    failed: 'Falhou',
    deferred: 'Adiado',
  })[status] || status
}

function metricLabel(name) {
  return ({
    commanderLegalityPassRate: 'Legalidade Commander',
    offColorDuplicateProtectedViolationRate: 'Violações objetivas',
    addPrecisionAt10: 'Precisão das adições',
    cutPrecisionAt10: 'Precisão dos cortes',
    preferenceAdherenceRate: 'Aderência às preferências',
    actionabilityRate: 'Aplicabilidade',
    blindPreferenceWinRate: 'Preferência na revisão cega',
  })[name] || name
}

function humanValidationLabel(status) {
  return status === 'completed' ? 'concluída' : 'pendente'
}

function comparisonStatusLabel(status) {
  return status === 'automatic_qualified_without_human_validation'
    ? 'Qualificação automática, sem validação humana'
    : statusLabel(status)
}
