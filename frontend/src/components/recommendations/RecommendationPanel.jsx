import RecommendationCard from './RecommendationCard'
import RecommendationBadge from './RecommendationBadge'
import StateMessage from '../ui/StateMessage'

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
}) {
  const items = Array.isArray(recommendations) ? recommendations : []
  const hasGenerated = Array.isArray(recommendations)
  const source = profileSource(metaProfile, items)
  const hasFallback = source === 'fallback'

  return (
    <section className="recommendation-panel" aria-live="polite">
      <header className="recommendation-panel-header">
        <div>
          <p className="eyebrow">Upgrade Path</p>
          <h3>Recomendacoes Estrategicas</h3>
          <p>Trocas sugeridas com base no comandante, bracket, curva, funcoes do deck e dados de listas similares quando disponiveis.</p>
        </div>
        <div className="recommendation-panel-context">
          <RecommendationBadge variant="curve">Bracket: {bracket}</RecommendationBadge>
          <RecommendationBadge variant={hasFallback ? 'fallback' : 'meta'}>
            {hasFallback ? 'Analise heuristica' : 'Perfil meta local'}
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
          <strong>{hasFallback ? 'Heuristica' : 'Meta local'}</strong>
        </div>
        <div>
          <span>Amostra meta</span>
          <strong>{metaProfile?.sampleSize ?? 0}</strong>
        </div>
      </div>

      {hasFallback && items.length > 0 && (
        <StateMessage tone="warning" title="Dados meta insuficientes">
          Usando analise heuristica: dados meta insuficientes para este comandante.
        </StateMessage>
      )}

      {loading && (
        <StateMessage title="Analisando sinergia, curva e dados de meta...">
          Isso normalmente leva poucos segundos.
        </StateMessage>
      )}

      {error && (
        <StateMessage tone="error" title="Nao foi possivel gerar recomendacoes">
          Tente novamente em instantes.
        </StateMessage>
      )}

      {!loading && !error && !hasGenerated && (
        <StateMessage title="Gere recomendacoes para visualizar trocas sugeridas.">
          Escolha o bracket e deixe o motor comparar o deck atual com o perfil do comandante.
        </StateMessage>
      )}

      {!loading && !error && hasGenerated && items.length === 0 && (
        <StateMessage tone="warning" title="Nenhuma troca segura foi montada nesta rodada.">
          O deck foi selecionado, mas faltou um par add/remove confiavel para exibir. Isso normalmente indica poucos candidatos validos ou cortes protegidos pelas regras do deck.
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
            />
          ))}
        </div>
      )}

      {metaSources.length > 0 && (
        <details className="recommendation-source-details">
          <summary>Ver fontes disponiveis</summary>
          <div>
            {metaSources.map((sourceItem) => (
              <span key={sourceItem.name} className={sourceItem.enabled ? 'source-pill enabled' : 'source-pill'}>
                {sourceItem.name}: {sourceItem.enabled ? 'ativo' : 'inativo'}
              </span>
            ))}
          </div>
        </details>
      )}
    </section>
  )
}
