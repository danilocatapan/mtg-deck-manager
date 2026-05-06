import React, { useEffect, useState } from 'react'
import { searchCards } from '../services/api'
import Button from './ui/Button'
import Input from './ui/Input'

export default function CardSearch({ onSelect }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [debouncing, setDebouncing] = useState(false)
  const [hasSearched, setHasSearched] = useState(false)

  async function doSearch(q = query) {
    const trimmed = q.trim()
    if (!trimmed) return

    console.log(`event=card.search query=${trimmed}`)
    setLoading(true)
    const res = await searchCards(trimmed)
    setResults(res)
    setLoading(false)
    setHasSearched(true)
  }

  useEffect(() => {
    if (!query.trim()) {
      setResults([])
      setHasSearched(false)
      return undefined
    }

    setDebouncing(true)
    const timer = setTimeout(() => {
      doSearch(query)
      setDebouncing(false)
    }, 350)

    return () => clearTimeout(timer)
  }, [query])

  function handleAdd(card) {
    if (onSelect) onSelect({ name: card.name })
  }

  return (
    <div className="card-search">
      <div className="search-row">
        <Input
          id="card-search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by card name"
          onKeyDown={async (e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              if (results.length > 0) {
                handleAdd(results[0])
              } else {
                await doSearch()
              }
            }
          }}
        />
        <Button type="button" onClick={() => doSearch()} disabled={!query.trim() || loading}>
          {loading || debouncing ? 'Searching...' : 'Search'}
        </Button>
      </div>

      <div className="search-meta">
        {loading || debouncing
          ? 'Searching Scryfall...'
          : hasSearched
            ? `${results.length} result${results.length === 1 ? '' : 's'}`
            : 'Type a card name to search and add it to this deck.'}
      </div>

      {results.length > 0 && (
        <ul className="result-list">
          {results.map((card, index) => (
            <li key={`${card.name}-${index}`} className="result-item">
              <div>
                <strong>{card.name}</strong>
                <div className="result-card-meta">
                  {card.manaCost || 'No cost'} / CMC {card.cmc ?? '-'} / {card.typeLine || 'Unknown type'}
                </div>
              </div>
              {onSelect && (
                <Button type="button" onClick={() => handleAdd(card)}>
                  Add
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}

      {hasSearched && !loading && !debouncing && results.length === 0 && (
        <p className="empty-inline">No cards found. Try the exact English card name.</p>
      )}
    </div>
  )
}
