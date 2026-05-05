import React from 'react'

export default function DeckAnalysis({ analysis }) {
  if (!analysis) return null

  return (
    <div style={{ textAlign: 'left', marginTop: 12 }}>
      <h3>Deck Analysis</h3>
      <div>Average CMC: {analysis.averageCmc}</div>
      <div>Total Cards: {analysis.totalCards}</div>
      <div>Ramp: {analysis.rampCount}</div>
      <div>Draw: {analysis.drawCount}</div>
      <div>Removal: {analysis.removalCount}</div>

      <h4>Mana Curve</h4>
      <ul>
        {analysis.manaCurve && Object.entries(analysis.manaCurve).map(([cmc, count]) => (
          <li key={cmc}>{cmc}: {count}</li>
        ))}
      </ul>
    </div>
  )
}
