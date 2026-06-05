import RecommendationCard from './RecommendationCard'
import RecommendationBadge from './RecommendationBadge'
import StateMessage from '../ui/StateMessage'
import Button from '../ui/Button'
import { useEffect, useState } from 'react'
import { submitRecommendationFeedback } from '../../services/api'
import { diagnosticEvents, diagnosticsEnabled, emitDiagnostic, setDiagnosticsEnabled, subscribeDiagnostics } from '../../services/diagnostics'

function profileSource(metaProfile, recommendations) {
  if (recommendations?.some((item) => item.source === 'meta_profile')) return 'meta'
  if (metaProfile?.sampleSize >= 3) return 'meta'
  return 'fallback'
}

function bracketLabel(bracket) {
  if (bracket === 'cedh') return 'cEDH'
  if (bracket === 'high-power') return 'High-power'
  if (bracket === 'mid') return 'Mid'
  return 'Casual'
}

export default function RecommendationPanel({
  recommendations,
  loading = false,
  error = null,
  bracket = 'casual',
  metaProfile = null,
  metaSources = [],
  onApplyRecommendation,
  onUndoRecommendation,
  applyingKey = null,
  appliedKeys = new Set(),
  comparison = null,
  history = [],
}) {
  const [feedbackStatus, setFeedbackStatus] = useState(null)
  const [feedbackLoading, setFeedbackLoading] = useState(null)
  const [feedbackError, setFeedbackError] = useState(null)
  const [diagnostics, setDiagnostics] = useState(() => diagnosticsEnabled())
  const [events, setEvents] = useState(() => diagnosticEvents())
  const run = Array.isArray(recommendations) ? null : recommendations
  const items = Array.isArray(recommendations) ? recommendations : recommendations?.recommendations || []
  const hasGenerated = Array.isArray(recommendations) || Boolean(recommendations?.recommendations)
  const source = profileSource(metaProfile, items)
  const hasFallback = source === 'fallback'
  const confidence = run?.confidence || (hasFallback ? 'low_confidence' : 'medium_confidence')
  const lowConfidence = confidence === 'low_confidence'
  const limitations = Array.isArray(run?.limitations) ? run.limitations : []

  useEffect(() => subscribeDiagnostics(() => {
    setDiagnostics(diagnosticsEnabled())
    setEvents(diagnosticEvents())
  }), [])

  async function handleFeedback(status) {
    if (!run?.auditId || feedbackLoading) return
    setFeedbackLoading(status)
    setFeedbackError(null)
    try {
      await submitRecommendationFeedback(run.auditId, status)
      setFeedbackStatus(status)
      emitDiagnostic('event=recommendation.feedback.recorded', { auditId: run.auditId, bracket, confidence, status })
    } catch {
      setFeedbackError('Nao foi possivel registrar sua avaliacao.')
    } finally {
      setFeedbackLoading(null)
    }
  }

  return (
    <section className="recommendation-panel" aria-live="polite">
      <header className="recommendation-panel-header">
        <div>
          <p className="eyebrow">Caminho de upgrade</p>
          <h3>Trocas recomendadas</h3>
          <p>Priorize poucas trocas claras: corrigir curva, mana, compra, interação ou encaixe com o comandante.</p>
        </div>
        <div className="recommendation-panel-context">
          <RecommendationBadge variant="curve">Bracket: {bracket}</RecommendationBadge>
          <RecommendationBadge variant={lowConfidence ? 'fallback' : 'meta'}>
            {confidenceLabel(confidence)}
          </RecommendationBadge>
          <RecommendationBadge variant={hasFallback ? 'fallback' : 'meta'}>
            {hasFallback ? 'Análise heurística' : 'Perfil meta local'}
          </RecommendationBadge>
        </div>
      </header>

      <div className="recommendation-context-strip">
        <div>
          <span>Trocas</span>
          <strong>{items.length || '-'}</strong>
        </div>
        <div>
          <span>Bracket</span>
          <strong>{bracketLabel(bracket)}</strong>
        </div>
        <div>
          <span>Origem</span>
          <strong>{hasFallback ? 'Heurística' : 'Meta local'}</strong>
        </div>
        <div>
          <span>Confianca</span>
          <strong>{shortConfidence(confidence)}</strong>
        </div>
      </div>

      {lowConfidence && (
        <StateMessage tone="warning" title="Ainda nao e possivel prometer melhor qualidade que GPT">
          Nao tenho dados suficientes para superar uma analise GPT ampla neste caso; posso ainda validar legalidade e sugerir melhorias conservadoras.
        </StateMessage>
      )}

      {hasFallback && items.length > 0 && (
        <StateMessage tone="warning" title="Dados meta insuficientes">
          Usando análise heurística: dados meta insuficientes para este comandante.
        </StateMessage>
      )}

      {loading && (
        <StateMessage title="Analisando sinergia, curva e dados de meta...">
          Isso normalmente leva poucos segundos.
        </StateMessage>
      )}

      {error && (
        <StateMessage tone="error" title="Não foi possível gerar recomendações">
          Tente novamente em instantes.
        </StateMessage>
      )}

      {!loading && !error && !hasGenerated && (
        <StateMessage title="Gere recomendações para visualizar trocas sugeridas.">
          Escolha o bracket e deixe o motor comparar o deck atual com o perfil do comandante.
        </StateMessage>
      )}

      {!loading && !error && hasGenerated && items.length === 0 && (
        <StateMessage tone="warning" title="Nenhuma troca segura foi montada nesta rodada.">
          O deck foi selecionado, mas faltou um par add/remove confiável para exibir. Isso normalmente indica poucos candidatos válidos ou cortes protegidos pelas regras do deck.
        </StateMessage>
      )}

      {items.length > 0 && (
        <div className="recommendation-card-list">
          {items.map((item, index) => (
            <RecommendationCard
              key={`${item.add}-${item.remove}-${index}`}
              item={item}
              index={index}
              bracket={item.bracket || bracket}
              onApply={onApplyRecommendation}
              onUndo={onUndoRecommendation}
              applying={applyingKey === recommendationKey(item)}
              applied={appliedKeys.has(recommendationKey(item))}
            />
          ))}
        </div>
      )}

      {run?.auditId && items.length > 0 && (
        <section className="card action-card recommendation-feedback" aria-live="polite">
          <div>
            <span className="rec-label">Ajude a melhorar</span>
            <h4>Esta rodada foi util?</h4>
            <p>O feedback entra no acompanhamento de qualidade e nao altera pesos automaticamente.</p>
          </div>
          <div className="actions-row">
            <Button variant={feedbackStatus === 'accepted' ? 'primary' : 'secondary'} loading={feedbackLoading === 'accepted'} onClick={() => handleFeedback('accepted')}>Util</Button>
            <Button variant={feedbackStatus === 'rejected' ? 'primary' : 'secondary'} loading={feedbackLoading === 'rejected'} onClick={() => handleFeedback('rejected')}>Nao util</Button>
            <Button variant={feedbackStatus === 'needs_review' ? 'primary' : 'secondary'} loading={feedbackLoading === 'needs_review'} onClick={() => handleFeedback('needs_review')}>Precisa revisao</Button>
          </div>
          {feedbackStatus && <span className="status-pill ready">Feedback registrado</span>}
          {feedbackError && <span className="status error">{feedbackError}</span>}
        </section>
      )}

      {(comparison?.metrics?.length > 0 || history.length > 0 || metaSources.length > 0 || run) && (
        <details className="recommendation-source-details advanced-recommendation-details">
          <summary>Informações avançadas</summary>

          {run && (
            <section className="recommendation-quality-details">
              <div className="section-heading compact">
                <div>
                  <p className="eyebrow">Qualidade da recomendacao</p>
                  <h4>{confidenceLabel(confidence)}</h4>
                </div>
                <RecommendationBadge variant={lowConfidence ? 'fallback' : 'meta'}>
                  {run.benchmarkStatus || 'benchmark pendente'}
                </RecommendationBadge>
              </div>
              <div className="quality-grid">
                <span className="source-pill enabled">Amostra: {run.coverage?.sampleSize ?? 0}</span>
                <span className="source-pill">Cartas: {run.coverage?.resolvedCards ?? 0}/{run.coverage?.requestedCards ?? 0}</span>
                <span className="source-pill">Dados: {formatFreshness(run.dataFreshness)}</span>
              </div>
              {limitations.length > 0 && (
                <ul className="quality-limitations">
                  {limitations.slice(0, 5).map((limitation) => (
                    <li key={limitation}>{limitation}</li>
                  ))}
                </ul>
              )}
            </section>
          )}

          {comparison?.metrics?.length > 0 && (
            <section className="recommendation-comparison">
              <div className="section-heading compact">
                <div>
                  <p className="eyebrow">Seu deck vs média do comandante</p>
                  <h4>{comparison.commander}</h4>
                </div>
                <RecommendationBadge variant="meta">Amostra: {comparison.sampleSize || 0}</RecommendationBadge>
              </div>
              <div className="comparison-grid">
                {comparison.metrics.slice(0, 5).map((metric) => (
                  <div key={metric.key} className={`comparison-metric ${metric.status}`}>
                    <span>{metric.label}</span>
                    <strong>{formatMetric(metric.deckValue)} / {formatMetric(metric.similarAverage)}</strong>
                    <small>{metric.message}</small>
                  </div>
                ))}
              </div>
            </section>
          )}

          {history.length > 0 && (
            <section>
              <h4>Histórico de trocas</h4>
              <div className="history-list">
                {history.slice().reverse().slice(0, 8).map((entry) => (
                  <span key={entry.id} className={entry.undone ? 'source-pill' : 'source-pill enabled'}>
                    {entry.undone ? 'Desfeita' : 'Aplicada'}: +{entry.add} / -{entry.remove}
                  </span>
                ))}
              </div>
            </section>
          )}

          {metaSources.length > 0 && (
            <section>
              <h4>Fontes disponíveis</h4>
              <div>
                {metaSources.map((sourceItem) => (
                  <span key={sourceItem.name} className={sourceItem.enabled ? 'source-pill enabled' : 'source-pill'}>
                    {sourceItem.name}: {sourceItem.enabled ? 'ativo' : 'inativo'}
                  </span>
                ))}
              </div>
            </section>
          )}
        </details>
      )}

      <details className="recommendation-source-details advanced-recommendation-details">
        <summary>Diagnostico da sessao</summary>
        <p>Exibe apenas IDs, status e contagens sanitizadas. Nenhum token, identidade ou decklist completa e registrado.</p>
        <Button variant="secondary" onClick={() => {
          const enabled = !diagnostics
          setDiagnosticsEnabled(enabled)
          if (enabled) emitDiagnostic('event=recommendation.diagnostics.enabled', { auditId: run?.auditId, bracket, confidence, count: items.length, limitations })
        }}>
          {diagnostics ? 'Desativar diagnostico' : 'Ativar diagnostico'}
        </Button>
        {diagnostics && (
          <ul className="quality-limitations" aria-label="Eventos de diagnostico">
            {events.slice(-8).map((event, index) => <li key={`${event.at}-${index}`}>{event.event} | {event.status || event.confidence || 'ok'} | {event.count ?? '-'}</li>)}
          </ul>
        )}
      </details>
    </section>
  )
}

function recommendationKey(item) {
  return item?.id || `${item?.add || ''}|||${item?.remove || ''}`.toLowerCase()
}

function formatMetric(value) {
  const numeric = Number(value || 0)
  return Number.isInteger(numeric) ? String(numeric) : numeric.toFixed(2)
}

function confidenceLabel(confidence) {
  if (confidence === 'high_confidence') return 'Confianca alta'
  if (confidence === 'medium_confidence') return 'Confianca media'
  return 'Confianca baixa'
}

function shortConfidence(confidence) {
  if (confidence === 'high_confidence') return 'Alta'
  if (confidence === 'medium_confidence') return 'Media'
  return 'Baixa'
}

function formatFreshness(value) {
  if (!value || value === 'unknown') return 'desconhecido'
  return String(value).slice(0, 10)
}
