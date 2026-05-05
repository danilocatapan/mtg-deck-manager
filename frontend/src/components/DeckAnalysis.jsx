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
