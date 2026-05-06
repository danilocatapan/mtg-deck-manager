import React from 'react'

export default function DeckAnalysis({ analysis }) {
  if (!analysis) return null

  const formatMetric = (value) => {
    if (typeof value !== 'number') return value ?? '-'
    if (!Number.isFinite(value)) return '-'
    return Number.isInteger(value) ? value : value.toFixed(2)
  }

  const metrics = [
    ['Average CMC', analysis.averageCmc],
    ['Total Cards', analysis.totalCards],
    ['Ramp', analysis.rampCount],
    ['Draw', analysis.drawCount],
    ['Removal', analysis.removalCount],
  ]
  const diagnostics = [
    { label: 'Early Game', value: (analysis.manaCurve?.[0] || 0) + (analysis.manaCurve?.[1] || 0) + (analysis.manaCurve?.[2] || 0), goodAt: 16 },
    { label: 'Ramp Density', value: analysis.rampCount ?? 0, goodAt: 10 },
    { label: 'Interaction', value: analysis.removalCount ?? 0, goodAt: 8 },
    { label: 'Card Advantage', value: analysis.drawCount ?? 0, goodAt: 8 },
  ].map((item) => ({
    ...item,
    tone: item.value >= item.goodAt ? 'good' : item.value >= Math.ceil(item.goodAt * 0.65) ? 'warning' : 'bad',
  }))

  return (
    <div className="analysis-panel">
      <div className="metric-grid">
        {metrics.map(([label, value]) => (
          <div key={label} className="metric-card">
            <span>{label}</span>
            <strong title={String(value)}>{formatMetric(value)}</strong>
          </div>
        ))}
      </div>

      <h4>Commander Diagnostics</h4>
      <div className="diagnostic-grid">
        {diagnostics.map((item) => (
          <div key={item.label} className={`diagnostic-card metric-${item.tone}`}>
            <span>{item.label}</span>
            <strong>{formatMetric(item.value)}</strong>
            <small>Target {item.goodAt}+</small>
          </div>
        ))}
      </div>

      <h4>Mana Curve</h4>
      <div className="mana-curve">
        {analysis.manaCurve && Object.entries(analysis.manaCurve).map(([cmc, count]) => (
          <div key={cmc} className="curve-row">
            <span>{cmc}</span>
            <div>
              <i style={{ width: `${Math.max(8, Number(count) * 16)}px` }} />
            </div>
            <strong>{count}</strong>
          </div>
        ))}
      </div>
    </div>
  )
}
