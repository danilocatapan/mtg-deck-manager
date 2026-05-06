import React from 'react'

export default function Recommendations({ rec }) {
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

      {rec.gaps && (
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

      {rec.add && rec.add.length > 0 && (
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

      {rec.cut && rec.cut.length > 0 && (
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
