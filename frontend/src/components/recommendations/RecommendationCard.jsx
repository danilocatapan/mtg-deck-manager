import Button from '../ui/Button'
import RecommendationBadge from './RecommendationBadge'

function inferTags(item) {
  if (Array.isArray(item?.tags) && item.tags.length > 0) return item.tags
  const text = String(item?.reasoning || '').toLowerCase()
  return [
    text.includes('lista') || text.includes('meta') ? 'meta' : null,
    text.includes('compra') || text.includes('draw') || text.includes('card advantage') ? 'draw' : null,
    text.includes('ramp') || text.includes('aceler') ? 'ramp' : null,
    text.includes('remoc') || text.includes('intera') ? 'removal' : null,
    text.includes('prote') ? 'protection' : null,
    text.includes('curva') ? 'curve' : null,
    text.includes('eficien') ? 'efficiency' : null,
    text.includes('sinerg') || text.includes('plano') ? 'synergy' : null,
  ].filter(Boolean)
}

function sourceLabel(source) {
  return source === 'meta_profile' ? 'Baseado em perfil meta local' : 'Baseado em analise heuristica'
}

function confidenceLabel(confidence) {
  if (confidence === 'high') return 'Alta'
  if (confidence === 'low') return 'Baixa'
  return 'Media'
}

function modeLabel(mode) {
  if (mode === 'budget') return 'Mais barato'
  if (mode === 'competitive') return 'Mais competitivo'
  if (mode === 'theme') return 'Mais fiel ao tema'
  if (mode === 'casual') return 'Mais casual'
  return 'Mais consistente'
}

function percent(value) {
  const numeric = Number(value || 0)
  return `${Math.round(numeric * 100)}%`
}

function price(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return 'Sem preco'
  return `US$ ${Number(value).toFixed(2)}`
}

function roleLabel(role) {
  if (role === 'draw') return 'Compra'
  if (role === 'ramp') return 'Ramp'
  if (role === 'removal') return 'Interacao'
  if (role === 'protection') return 'Protecao'
  if (role === 'finisher') return 'Wincon'
  return role || 'Valor'
}

function opportunityFrom(reasoning) {
  const firstSentence = String(reasoning || '').split('.').map((part) => part.trim()).find(Boolean)
  return firstSentence || 'Troca sugerida para melhorar o encaixe do deck no bracket escolhido'
}

export default function RecommendationCard({ item, index, bracket, onApply, applying = false, applied = false }) {
  const source = item?.source || (String(item?.reasoning || '').toLowerCase().includes('listas similares') ? 'meta_profile' : 'heuristic_fallback')
  const tags = inferTags(item).filter((tag) => tag !== 'fallback' && tag !== 'meta')
  const confidence = item?.confidence || 'medium'
  const addInsight = item?.addInsight
  const impact = item?.impact
  const comparisons = Array.isArray(item?.comparisons) ? item.comparisons : []

  return (
    <article className="recommendation-card-pro">
      <header className="recommendation-card-header">
        <div>
          <span className="rec-label">Recomendacao #{index + 1}</span>
          <h4>{sourceLabel(source)}</h4>
        </div>
        <div className="recommendation-card-badges" aria-label="Motivos da recomendacao">
          <RecommendationBadge variant={source === 'meta_profile' ? 'meta' : 'fallback'} />
          <RecommendationBadge variant={confidence} />
          {tags.map((tag) => <RecommendationBadge key={tag} variant={tag} />)}
        </div>
      </header>

      <section className="recommendation-opportunity">
        <span className="rec-label">Oportunidade identificada</span>
        <p>{opportunityFrom(item?.reasoning)}</p>
      </section>

      <div className="swap-route" aria-label="Troca sugerida">
        <section className="swap-card add">
          <span>Adicionar</span>
          <strong>+ {item.add}</strong>
        </section>
        <span className="swap-arrow" aria-hidden="true">-&gt;</span>
        <section className="swap-card remove">
          <span>Remover</span>
          <strong>- {item.remove}</strong>
        </section>
      </div>

      <section className="reasoning-block">
        <span className="rec-label">Por que essa troca faz sentido</span>
        <p>{item.reasoning}</p>
      </section>

      {(addInsight || impact || comparisons.length > 0) && (
        <section className="recommendation-explainability">
          {addInsight && (
            <div>
              <span className="rec-label">Carta sugerida</span>
              <strong>{roleLabel(addInsight.role)} | {percent(addInsight.inclusionRate)} inclusao</strong>
              <p>
                {addInsight.sampleSize > 0 ? `${addInsight.sampleSize} listas similares` : 'Base heuristica local'}
                {' | '}
                Fonte: {addInsight.source || source}
                {' | '}
                {percent(addInsight.synergyEstimate)} sinergia estimada
                {' | '}
                {price(addInsight.estimatedPrice)}
              </p>
              {addInsight.priceDisclaimer && <small>{addInsight.priceDisclaimer}</small>}
            </div>
          )}

          {impact && (
            <div>
              <span className="rec-label">Impacto esperado</span>
              <strong>{roleLabel(impact.role)}</strong>
              <p>
                CMC medio {Number(impact.averageCmcBefore || 0).toFixed(2)}
                {' para '}
                {Number(impact.averageCmcAfter || 0).toFixed(2)}
                {' | '}
                Ramp {impact.rampBefore ?? '-'} para {impact.rampAfter ?? '-'}
                {' | '}
                Compra {impact.drawBefore ?? '-'} para {impact.drawAfter ?? '-'}
                {' | '}
                Interacao {impact.removalBefore ?? '-'} para {impact.removalAfter ?? '-'}
              </p>
            </div>
          )}

          {comparisons.map((comparison) => (
            <div key={`${comparison.role}-${comparison.targetCount}`}>
              <span className="rec-label">Comparacao com listas similares</span>
              <p>{comparison.message}</p>
            </div>
          ))}
        </section>
      )}

      <footer className="recommendation-card-footer">
        <span>Bracket: {item.bracket || bracket || 'casual'}</span>
        <span>Confianca: {confidenceLabel(confidence)}</span>
        <span>Modo: {modeLabel(item.recommendationMode)}</span>
        <details>
          <summary>Detalhes</summary>
          <p>
            {sourceLabel(source)}.
            {item?.sourceContext?.sampleSize ? ` Amostra: ${item.sourceContext.sampleSize} listas.` : ''}
            {item?.sourceContext?.sources?.length ? ` Fontes: ${item.sourceContext.sources.join(', ')}.` : ''}
            {` Confianca ${confidenceLabel(confidence).toLowerCase()}: ${confidenceReason(confidence, item?.sourceContext?.sampleSize, source)}`}
          </p>
        </details>
        <Button
          variant={applied ? 'secondary' : 'primary'}
          loading={applying}
          disabled={applied || !item?.add || !item?.remove}
          onClick={() => onApply && onApply(item)}
        >
          {applied ? 'Aplicado' : 'Aplicar troca'}
        </Button>
      </footer>
    </article>
  )
}

function confidenceReason(confidence, sampleSize = 0, source = '') {
  if (confidence === 'high') return 'amostra meta suficiente e encaixe forte.'
  if (confidence === 'low' && source !== 'meta_profile') return 'fallback heuristico com pouca evidencia externa.'
  if (confidence === 'low') return `amostra pequena (${sampleSize || 0}) ou score abaixo do alvo.`
  return source === 'meta_profile' ? 'dados de meta disponiveis, mas ainda com margem de revisao.' : 'heuristica local com score aceitavel.'
}
