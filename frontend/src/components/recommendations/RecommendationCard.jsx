import Button from '../ui/Button'
import CardNamePreview from '../CardNamePreview'
import DisclosureCard from '../ui/DisclosureCard'

function sourceLabel(source) {
  return source === 'meta_profile' ? 'meta local' : 'heuristica'
}

function confidenceLabel(confidence) {
  if (confidence === 'high') return 'alta'
  if (confidence === 'low') return 'baixa'
  return 'media'
}

function roleLabel(role) {
  if (role === 'draw') return 'compra'
  if (role === 'ramp') return 'ramp'
  if (role === 'removal') return 'interacao'
  if (role === 'protection') return 'protecao'
  if (role === 'finisher') return 'condicao de vitoria'
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
  const resolvedBracket = item.bracket || bracket || 'casual'

  return (
    <DisclosureCard
      className="recommendation-card-pro compact-recommendation-card"
      title={(
        <span>
          <span className="rec-label">Troca #{index + 1}</span>
          <span className="recommendation-title-line">+ {item.add}</span>
          <span className="recommendation-title-muted">Remover: {item.remove}</span>
        </span>
      )}
      summary="Abrir ou recolher detalhes da troca"
      meta={(
        <div className="compact-rec-meta">
          <span>{roleLabel(item?.impact?.role)}</span>
          <span>confianca {confidenceLabel(confidence)}</span>
        </div>
      )}
    >
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

      <section className="recommendation-safety">
        <span className="rec-label">Por que esta troca e segura</span>
        <ul>
          <li>Identidade de cor e duplicatas seguem os filtros do backend.</li>
          <li>O comandante nao entra como corte e a troca fica registrada no historico.</li>
          <li>Bracket {resolvedBracket} e fonte {sourceLabel(source)} calibram o contexto da sugestao.</li>
          {item?.sourceContext?.sampleSize
            ? <li>Amostra usada: {item.sourceContext.sampleSize} listas.</li>
            : <li>Sem amostra meta suficiente: heuristica conservadora.</li>}
        </ul>
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
        <span>Bracket: {resolvedBracket}</span>
        <span>Fonte: {sourceLabel(source)}</span>
        {item?.sourceContext?.sampleSize ? <span>Amostra: {item.sourceContext.sampleSize} listas</span> : null}
        <Button
          variant={applied ? 'secondary' : 'primary'}
          loading={applying}
          loadingLabel="Aplicando troca..."
          disabled={applied || (!item?.add || !item?.remove) || applying}
          onClick={() => onApply && onApply(item)}
        >
          {applied ? 'Aplicado' : 'Aplicar troca'}
        </Button>
        {applied && (
          <Button
            variant="secondary"
            loading={applying}
            loadingLabel="Desfazendo troca..."
            disabled={applying}
            onClick={() => onUndo && onUndo(item)}
          >
            Desfazer
          </Button>
        )}
      </footer>
    </DisclosureCard>
  )
}

function impactHighlights(impact) {
  if (!impact) return []
  return [
    makeHighlight('Curva', impact.averageCmcBefore, impact.averageCmcAfter, false, 2),
    makeHighlight('Ramp', impact.rampBefore, impact.rampAfter, true),
    makeHighlight('Compra', impact.drawBefore, impact.drawAfter, true),
    makeHighlight('Interacao', impact.removalBefore, impact.removalAfter, true),
    makeHighlight('Game Changers', impact.gameChangersBefore, impact.gameChangersAfter, false),
    makeHighlight('Pressao bracket', impact.bracketPressureBefore, impact.bracketPressureAfter, false),
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
