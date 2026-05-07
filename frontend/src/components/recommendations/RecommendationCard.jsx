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

function opportunityFrom(reasoning) {
  const firstSentence = String(reasoning || '').split('.').map((part) => part.trim()).find(Boolean)
  return firstSentence || 'Troca sugerida para melhorar o encaixe do deck no bracket escolhido'
}

export default function RecommendationCard({ item, index, bracket, onApply, applying = false, applied = false }) {
  const source = item?.source || (String(item?.reasoning || '').toLowerCase().includes('listas similares') ? 'meta_profile' : 'heuristic_fallback')
  const tags = inferTags(item).filter((tag) => tag !== 'fallback' && tag !== 'meta')
  const confidence = item?.confidence || 'medium'

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

      <footer className="recommendation-card-footer">
        <span>Bracket: {item.bracket || bracket || 'casual'}</span>
        <span>Confianca: {confidenceLabel(confidence)}</span>
        <details>
          <summary>Detalhes</summary>
          <p>{sourceLabel(source)}.</p>
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
