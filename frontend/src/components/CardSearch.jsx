import React, { useState, useEffect } from 'react'
import { searchCards } from '../services/api'
import Button from './ui/Button'
import Input from './ui/Input'

export default function CardSearch({ onSelect }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [debouncing, setDebouncing] = useState(false)

  async function doSearch(q = query) {
    console.log(`event=card.search query=${q}`)
    setLoading(true)
    const res = await searchCards(q)
    setResults(res)
    setLoading(false)
  }

  // debounce automatic search
  useEffect(() => {
    if (!query) return setResults([])
    setDebouncing(true)
    const t = setTimeout(() => {
      doSearch(query)
      setDebouncing(false)
    }, 300)
    return () => clearTimeout(t)
  }, [query])

  function handleAdd(card) {
    if (onSelect) onSelect({ name: card.name })
  }

  return (
    <div style={{ textAlign: 'left' }}>
      <div>
        <Input
          id="card-search"
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
        <Button type="button" onClick={() => doSearch()} disabled={!query || loading} className="" style={{ marginLeft: 8 }}>
          {loading || debouncing ? 'Searching...' : 'Search'}
        </Button>
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
