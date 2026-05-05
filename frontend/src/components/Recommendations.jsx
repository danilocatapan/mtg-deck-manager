import React from 'react'

export default function Recommendations({ rec }) {
  if (!rec) return null

  const recommendationKey = (item, index) => `${item.name}-${item.role || 'unknown'}-${item.reason || 'none'}-${index}`
  const quantity = (item) => item.quantity ?? 1

  return (
    <div style={{ textAlign: 'left', marginTop: 12 }}>
      <h3>Recommendations</h3>

      {rec.gaps && (
        <div>
          <h4>Gaps</h4>
          <ul>
            {Object.entries(rec.gaps).map(([k, v]) => (
              <li key={k}>{k}: {v}</li>
            ))}
          </ul>
        </div>
      )}

      {rec.add && rec.add.length > 0 && (
        <div>
          <h4>Add</h4>
          <ul>
            {rec.add.map((i, index) => (
              <li key={recommendationKey(i, index)}>{quantity(i)} x {i.name} - score: {i.score} - {i.reason}</li>
            ))}
          </ul>
        </div>
      )}

      {rec.cut && rec.cut.length > 0 && (
        <div>
          <h4>Cut</h4>
          <ul>
            {rec.cut.map((i, index) => (
              <li key={recommendationKey(i, index)}>{quantity(i)} x {i.name} - reason: {i.reason}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
