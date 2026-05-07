function motivationBadges(reasoning = '') {
  const text = reasoning.toLowerCase()
  return [
    text.includes('sinerg') || text.includes('plano') ? 'synergy' : null,
    text.includes('precisa') || text.includes('lacuna') || text.includes('gap') ? 'gap' : null,
    text.includes('eficient') || text.includes('curva') ? 'efficiency' : null,
    text.includes('meta') || text.includes('performance') ? 'meta' : null,
  ].filter(Boolean)
}

function MetaContext({ metaProfile, metaSources }) {
  if (!metaProfile && (!metaSources || metaSources.length === 0)) return null
  const sourcesUsed = metaProfile?.sourcesUsed || []
  return (
    <section className="meta-context-panel">
      <div>
        <span className="rec-label">Contexto usado</span>
        <h4>{metaProfile?.bracket || 'auto'} / {metaProfile?.sourceMode || 'auto'}</h4>
      </div>
      <div className="meta-context-grid">
        <div>
          <dt>Amostra local</dt>
          <dd>{metaProfile?.sampleSize ?? 0} cartas priorizadas</dd>
        </div>
        <div>
          <dt>Fontes escolhidas</dt>
          <dd>{sourcesUsed.length ? sourcesUsed.join(', ') : 'LOCAL'}</dd>
        </div>
        <div>
          <dt>Modo de runtime</dt>
          <dd>cache local, sem chamada externa direta</dd>
        </div>
      </div>
      {metaSources?.length > 0 && (
        <div className="source-status-list">
          {metaSources.map((source) => (
            <span key={source.name} className={source.enabled ? 'source-pill enabled' : 'source-pill'}>
              {source.name}: {source.enabled ? 'ativo' : 'inativo'}
            </span>
          ))}
        </div>
      )}
    </section>
  )
}

function StrategicRecommendations({ rec }) {
  if (!Array.isArray(rec)) return null
  if (rec.length === 0) {
    return <div className="empty-inline">Nenhuma troca estratégica segura foi encontrada com os filtros atuais.</div>
  }

  return (
    <div className="strategic-swap-list">
      {rec.map((item, index) => {
        const badges = motivationBadges(item.reasoning)
        return (
          <article key={`${item.add}-${item.remove}-${index}`} className="strategic-swap-card">
            <div className="swap-summary">
              <span className="rec-label">Troca recomendada</span>
              <strong>{item.reasoning}</strong>
            </div>
            <div className="motivation-badges">
              {(badges.length ? badges : ['strategy']).map((badge) => <span key={badge}>{badge}</span>)}
            </div>
          </article>
        )
      })}
    </div>
  )
}

export default function Recommendations({ rec, metaProfile, metaSources = [] }) {
  if (!rec) return null

  const recommendationKey = (item, index) => `${item.name}-${item.role || 'unknown'}-${item.reason || 'none'}-${index}`
  const quantity = (item) => item.quantity ?? 1
  const impactFor = (item, fallback) => item.impact || item.role || fallback
  const priorityFor = (item) => {
    if (item.priority) return item.priority
    if (Number(item.score) >= 0.8) return 'high'
    if (Number(item.score) >= 0.45) return 'medium'
    return 'medium'
  }

  return (
    <div className="recommendation-results">
      <h3>Recommendations</h3>
      <MetaContext metaProfile={metaProfile} metaSources={metaSources} />
      {Array.isArray(rec) && <StrategicRecommendations rec={rec} />}

      {!Array.isArray(rec) && rec.gaps && (
        <div className="gap-panel">
          <h4>Gaps</h4>
          <div className="gap-grid">
            {Object.entries(rec.gaps).map(([k, v]) => (
              <div key={k} className="gap-item">
                <span>{k}</span>
                <strong>{v}</strong>
              </div>
            ))}
          </div>
        </div>
      )}

      {!Array.isArray(rec) && rec.add && rec.add.length > 0 && (
        <div>
          <h4>Add</h4>
          <div className="recommendation-grid">
            {rec.add.map((i, index) => (
              <article key={recommendationKey(i, index)} className="recommendation-card">
                <div>
                  <span className="rec-label">Suggested card</span>
                  <h5>{quantity(i)} x {i.name}</h5>
                </div>
                <dl>
                  <div><dt>Strategic role</dt><dd>{i.role || 'Upgrade slot'}</dd></div>
                  <div><dt>Reason</dt><dd>{i.reason || 'Improves deck consistency.'}</dd></div>
                  <div><dt>Expected impact</dt><dd>{impactFor(i, '+consistency')}</dd></div>
                  <div><dt>Priority</dt><dd>{priorityFor(i)}</dd></div>
                </dl>
              </article>
            ))}
          </div>
        </div>
      )}

      {!Array.isArray(rec) && rec.cut && rec.cut.length > 0 && (
        <div>
          <h4>Cut</h4>
          <div className="recommendation-grid">
            {rec.cut.map((i, index) => (
              <article key={recommendationKey(i, index)} className="recommendation-card cut-card">
                <div>
                  <span className="rec-label">Cut candidate</span>
                  <h5>{quantity(i)} x {i.name}</h5>
                </div>
                <dl>
                  <div><dt>Strategic role</dt><dd>{i.role || 'Lower-impact slot'}</dd></div>
                  <div><dt>Reason</dt><dd>{i.reason || 'Frees space for a stronger plan.'}</dd></div>
                  <div><dt>Expected impact</dt><dd>{impactFor(i, '+focus')}</dd></div>
                  <div><dt>Priority</dt><dd>{priorityFor(i)}</dd></div>
                </dl>
              </article>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
