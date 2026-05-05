import React, { useState } from 'react'
import Button from './ui/Button'

export default function RecommendationForm({ onSubmit, disabled = false }) {
  const [budget, setBudget] = useState(5)
  const [bracket, setBracket] = useState('casual')
  const [strategy, setStrategy] = useState('')

  function handleSubmit(e) {
    e.preventDefault()
    onSubmit({ budget, bracket, strategy })
  }

  return (
    <form onSubmit={handleSubmit} className="recommendation-form">
      <label title="Approximate budget available for suggested cards.">
        Budget
        <small>Estimated money available for additions.</small>
        <input type="number" min={0} value={budget} onChange={(e) => setBudget(parseInt(e.target.value || '0', 10))} />
      </label>

      <label title="Power level used to calculate expected ramp, draw and removal.">
        Bracket
        <small>Casual is slower, high expects stronger deck structure.</small>
        <select value={bracket} onChange={(e) => setBracket(e.target.value)}>
          <option value="casual">casual</option>
          <option value="mid">mid</option>
          <option value="high">high</option>
        </select>
      </label>

      <label title="Optional plan for recommendation flavor, such as aggro, counters, tokens or graveyard.">
        Strategy
        <small>Optional direction for recommendations.</small>
        <input value={strategy} onChange={(e) => setStrategy(e.target.value)} placeholder="aggro, tokens, ramp..." />
      </label>

      <Button type="submit" disabled={disabled}>Get Recommendations</Button>
    </form>
  )
}
