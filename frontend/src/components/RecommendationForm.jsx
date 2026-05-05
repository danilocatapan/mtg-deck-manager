import React, { useState } from 'react'

export default function RecommendationForm({ onSubmit }) {
  const [budget, setBudget] = useState(5)
  const [bracket, setBracket] = useState('casual')
  const [strategy, setStrategy] = useState('')

  function handleSubmit(e) {
    e.preventDefault()
    onSubmit({ budget, bracket, strategy })
  }

  return (
    <form onSubmit={handleSubmit} style={{ textAlign: 'left', marginTop: 12 }}>
      <div>
        <label>Budget</label>
        <br />
        <input type="number" value={budget} onChange={(e) => setBudget(parseInt(e.target.value || '0', 10))} />
      </div>

      <div style={{ marginTop: 8 }}>
        <label>Bracket</label>
        <br />
        <select value={bracket} onChange={(e) => setBracket(e.target.value)}>
          <option value="casual">casual</option>
          <option value="mid">mid</option>
          <option value="high">high</option>
        </select>
      </div>

      <div style={{ marginTop: 8 }}>
        <label>Strategy</label>
        <br />
        <input value={strategy} onChange={(e) => setStrategy(e.target.value)} />
      </div>

      <div style={{ marginTop: 12 }}>
        <button type="submit">Get Recommendations</button>
      </div>
    </form>
  )
}
