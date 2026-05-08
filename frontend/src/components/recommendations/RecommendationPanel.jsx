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
  onApplyRecommendation,
  onUndoRecommendation,
  applyingKey = null,
  appliedKeys = new Set(),
  comparison = null,
  packages = [],
  history = [],
  onAddPackage,
}) {
  const items = Array.isArray(recommendations) ? recommendations : []
  const hasGenerated = Array.isArray(recommendations)
  const source = profileSource(metaProfile, items)
  const hasFallback = source === 'fallback'

  return (
    <section className="recommendation-panel" aria-live="polite">
      <header className="recommendation-panel-header">
        <div>
          <p className="eyebrow">Caminho de upgrade</p>
          <h3>Trocas recomendadas</h3>
          <p>Priorize poucas trocas claras: corrigir curva, mana, compra, interacao ou encaixe com o comandante.</p>
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
          {items.slice(0, 5).map((item, index) => (
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

      {(comparison?.metrics?.length > 0 || packages.length > 0 || history.length > 0 || metaSources.length > 0) && (
        <details className="recommendation-source-details advanced-recommendation-details">
          <summary>Informacoes avancadas</summary>

          {comparison?.metrics?.length > 0 && (
            <section className="recommendation-comparison">
              <div className="section-heading compact">
                <div>
                  <p className="eyebrow">Seu deck vs media do comandante</p>
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

          {packages.length > 0 && (
            <section className="recommendation-packages">
              <div className="section-heading compact">
                <div>
                  <p className="eyebrow">Maybeboard</p>
                  <h4>Pacotes sugeridos</h4>
                </div>
              </div>
              <div className="package-grid">
                {packages.slice(0, 3).map((deckPackage) => (
                  <article key={deckPackage.id} className="package-card">
                    <strong>{deckPackage.name}</strong>
                    <p>{deckPackage.description}</p>
                    <small>{(deckPackage.cards || []).map((card) => card.name).join(', ')}</small>
                    <button type="button" onClick={() => onAddPackage && onAddPackage(deckPackage.id)}>
                      Adicionar ao maybeboard
                    </button>
                  </article>
                ))}
              </div>
            </section>
          )}

          {history.length > 0 && (
            <section>
              <h4>Historico de trocas</h4>
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
              <h4>Fontes disponiveis</h4>
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
