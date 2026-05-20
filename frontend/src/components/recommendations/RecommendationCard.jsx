import Button from '../ui/Button'
import CardNamePreview from '../CardNamePreview'

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
  const highlights = impactHighlights(item?.impact).slice(0, 6)
  const reason = item?.problem || opportunityFrom(item?.reasoning)
  const expected = item?.reasoning || 'Troca sugerida para deixar o deck mais consistente no bracket escolhido.'

  return (
    <article className="recommendation-card-pro compact-recommendation-card">
      <header className="recommendation-card-header">
        <div>
          <span className="rec-label">Troca #{index + 1}</span>
          <h4><CardNamePreview prefix="+ " name={item.add} /></h4>
          <p>Remover: <CardNamePreview name={item.remove} /></p>
        </div>
        <div className="compact-rec-meta">
          <span>{roleLabel(item?.impact?.role)}</span>
          <span>confiança {confidenceLabel(confidence)}</span>
        </div>
      </header>

      <section className="recommendation-swap-route" aria-label="Troca recomendada">
        <div className="swap-card remove">
          <span>Sai</span>
          <strong><CardNamePreview name={item.remove} /></strong>
        </div>
        <div className="swap-card add">
          <span>Entra</span>
          <strong><CardNamePreview name={item.add} /></strong>
        </div>
      </section>

      <section className="recommendation-opportunity">
        <span className="rec-label">Por que mexer</span>
        <p>{reason}</p>
      </section>

      <section className="recommendation-result">
        <span className="rec-label">Resultado esperado</span>
        <p>{expected}</p>
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

      {item?.risk && (
        <section className="recommendation-risk">
          <span className="rec-label">Risco</span>
          <p>{item.risk}</p>
        </section>
      )}

      <footer className="recommendation-card-footer">
        <span>Bracket: {item.bracket || bracket || 'casual'}</span>
        <span>Fonte: {sourceLabel(source)}</span>
        {item?.sourceContext?.sampleSize ? <span>Amostra: {item.sourceContext.sampleSize} listas</span> : null}
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
