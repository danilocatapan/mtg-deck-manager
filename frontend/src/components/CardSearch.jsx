import React, { useState } from 'react'
import { searchCards } from '../services/api'

export default function CardSearch({ onSelect }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)

  async function doSearch() {
    setLoading(true)
    const res = await searchCards(query)
    setResults(res)
    setLoading(false)
  }

  function handleAdd(card) {
    if (onSelect) onSelect({ name: card.name })
  }

  return (
    <div style={{ textAlign: 'left' }}>
      <div>
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Card name"
          onKeyDown={async (e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              if (results && results.length > 0) {
                handleAdd(results[0])
              } else {
                await doSearch()
              }
            }
          }}
        />
        <button type="button" onClick={doSearch} disabled={!query || loading} style={{ marginLeft: 8 }}>
          {loading ? 'Searching...' : 'Search'}
        </button>
      </div>

      <div style={{ marginTop: 12 }}>
        {results && results.length > 0 ? (
          <ul>
            {results.map((c) => (
              <li key={c.name} style={{ marginBottom: 6 }}>
                <span style={{ marginRight: 8 }}>{c.name} — {c.manaCost} — {c.typeLine}</span>
                {onSelect && (
                  <button type="button" onClick={() => handleAdd(c)} style={{ marginLeft: 8 }}>Add</button>
                )}
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
