import Button from '../ui/Button'

function sourceLabel(source) {
  return source === 'meta_profile' ? 'meta local' : 'heurística'
}

function confidenceLabel(confidence) {
  if (confidence === 'high') return 'alta'
  if (confidence === 'low') return 'baixa'
  return 'média'
}

function roleLabel(role) {
  if (role === 'draw') return 'compra'
  if (role === 'ramp') return 'ramp'
  if (role === 'removal') return 'interação'
  if (role === 'protection') return 'proteção'
  if (role === 'finisher') return 'condição de vitória'
  return role || 'valor'
}

function opportunityFrom(reasoning) {
  const firstSentence = String(reasoning || '').split('.').map((part) => part.trim()).find(Boolean)
  return firstSentence || 'Troca sugerida para melhorar o encaixe do deck no bracket escolhido.'
}

export default function RecommendationCard({ item, index, bracket, onApply, onUndo, applying = false, applied = false }) {
  const source = item?.source || 'heuristic_fallback'
  const confidence = item?.confidence || 'medium'
  const highlights = impactHighlights(item?.impact).slice(0, 3)

  return (
    <article className="recommendation-card-pro compact-recommendation-card">
      <header className="recommendation-card-header">
        <div>
          <span className="rec-label">Troca #{index + 1}</span>
          <h4>+ {item.add}</h4>
          <p>Remover: <strong>{item.remove}</strong></p>
        </div>
        <div className="compact-rec-meta">
          <span>{roleLabel(item?.impact?.role)}</span>
          <span>confiança {confidenceLabel(confidence)}</span>
        </div>
      </header>

      <section className="recommendation-opportunity">
        <span className="rec-label">Por que mexer</span>
        <p>{item?.problem || opportunityFrom(item?.reasoning)}</p>
      </section>

      {highlights.length > 0 && (
        <section className="compact-impact-list" aria-label="Principais impactos da troca">
          {highlights.map((highlight) => (
            <span key={highlight.label} className={`impact-pill ${highlight.tone}`}>
              <strong>{highlight.label}</strong>
              {highlight.text}
            </span>
          ))}
        </section>
      )}

      <footer className="recommendation-card-footer">
        <span>Bracket: {item.bracket || bracket || 'casual'}</span>
        <span>Fonte: {sourceLabel(source)}</span>
        <details>
          <summary>Detalhes</summary>
          <p>{item.reasoning}</p>
          {item?.risk && <p>{item.risk}</p>}
          {item?.sourceContext?.sampleSize ? <p>Amostra: {item.sourceContext.sampleSize} listas.</p> : null}
        </details>
        <Button
          variant={applied ? 'secondary' : 'primary'}
          loading={applying}
          disabled={applied || (!item?.add || !item?.remove) || applying}
          onClick={() => onApply && onApply(item)}
        >
          {applied ? 'Aplicado' : 'Aplicar troca'}
        </Button>
        {applied && (
          <Button
            variant="secondary"
            loading={applying}
            disabled={applying}
            onClick={() => onUndo && onUndo(item)}
          >
            Desfazer
          </Button>
        )}
      </footer>
    </article>
  )
}

function impactHighlights(impact) {
  if (!impact) return []
  return [
    makeHighlight('Curva', impact.averageCmcBefore, impact.averageCmcAfter, false, 2),
    makeHighlight('Ramp', impact.rampBefore, impact.rampAfter, true),
    makeHighlight('Compra', impact.drawBefore, impact.drawAfter, true),
    makeHighlight('Interação', impact.removalBefore, impact.removalAfter, true),
    makeHighlight('Game Changers', impact.gameChangersBefore, impact.gameChangersAfter, false),
    makeHighlight('Pressão bracket', impact.bracketPressureBefore, impact.bracketPressureAfter, false),
  ].filter(Boolean)
}

function makeHighlight(label, before, after, higherIsBetter, digits = 0) {
  const left = Number(before)
  const right = Number(after)
  if (!Number.isFinite(left) || !Number.isFinite(right) || Math.abs(right - left) < 0.005) {
    return null
  }
  const improved = higherIsBetter ? right > left : right < left
  const formattedBefore = formatNumber(left, digits)
  const formattedAfter = formatNumber(right, digits)
  return {
    label,
    text: `${formattedBefore} -> ${formattedAfter}`,
    tone: improved ? 'positive' : 'negative',
  }
}

function formatNumber(value, digits) {
  return digits > 0 ? value.toFixed(digits) : String(Math.round(value))
}
