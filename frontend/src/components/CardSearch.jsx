import React, { useState } from 'react'
import { searchCards } from '../services/api'

export default function CardSearch() {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)

  async function doSearch() {
    setLoading(true)
    const res = await searchCards(query)
    setResults(res)
    setLoading(false)
  }

  return (
    <div style={{ textAlign: 'left' }}>
      <div>
        <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Card name" />
        <button onClick={doSearch} disabled={!query || loading} style={{ marginLeft: 8 }}>
          {loading ? 'Searching...' : 'Search'}
        </button>
      </div>

      <div style={{ marginTop: 12 }}>
        {results && results.length > 0 ? (
          <ul>
            {results.map((c) => (
              <li key={c.name}>
                {c.name} — {c.manaCost} — {c.typeLine}
              </li>
            ))}
          </ul>
        ) : (
          <p>No results</p>
        )}
      </div>
    </div>
  )
}
