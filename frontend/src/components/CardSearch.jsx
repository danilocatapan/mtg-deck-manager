import { useCallback, useEffect, useState } from 'react'
import { searchCards } from '../services/api'
import Button from './ui/Button'
import Input from './ui/Input'

export default function CardSearch({ onSelect }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [debouncing, setDebouncing] = useState(false)
  const [hasSearched, setHasSearched] = useState(false)

  const doSearch = useCallback(async (q) => {
    const trimmed = q.trim()
    if (!trimmed) return

    console.log(`event=card.search query=${trimmed}`)
    setLoading(true)
    const res = await searchCards(trimmed)
    setResults(res)
    setLoading(false)
    setHasSearched(true)
  }, [])

  useEffect(() => {
    if (!query.trim()) return undefined

    const timer = setTimeout(() => {
      doSearch(query).finally(() => setDebouncing(false))
    }, 350)

    return () => clearTimeout(timer)
  }, [doSearch, query])

  function handleAdd(card) {
    if (onSelect) onSelect(card)
  }

  return (
    <div className="card-search">
      <div className="search-row">
        <Input
          id="card-search"
          value={query}
          onChange={(e) => {
            const nextQuery = e.target.value
            setQuery(nextQuery)
            if (!nextQuery.trim()) {
              setResults([])
              setHasSearched(false)
              setDebouncing(false)
            } else {
              setDebouncing(true)
            }
          }}
          placeholder="Buscar pelo nome da carta"
          onKeyDown={async (e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              if (results.length > 0) {
                handleAdd(results[0])
              } else {
                await doSearch(query)
              }
            }
          }}
        />
        <Button type="button" onClick={() => doSearch(query)} disabled={!query.trim() || loading}>
          {loading || debouncing ? 'Buscando...' : 'Buscar'}
        </Button>
      </div>

      <div className="search-meta">
        {loading || debouncing
          ? 'Buscando no Scryfall...'
          : hasSearched
            ? `${results.length} resultado${results.length === 1 ? '' : 's'}`
            : 'Digite o nome de uma carta para buscar e adicionar ao deck.'}
      </div>

      {results.length > 0 && (
        <ul className="result-list">
          {results.map((card, index) => (
            <li key={`${card.name}-${index}`} className="result-item">
              <div>
                <strong>{card.name}</strong>
                <div className="result-card-meta">
                  {card.manaCost || 'Sem custo'} / CMC {card.cmc ?? '-'} / {card.typeLine || 'Tipo desconhecido'}
                </div>
              </div>
              {onSelect && (
                <Button type="button" onClick={() => handleAdd(card)}>
                  Adicionar
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}

      {hasSearched && !loading && !debouncing && results.length === 0 && (
        <p className="empty-inline">Nenhuma carta encontrada. Tente o nome exato em ingles.</p>
      )}
    </div>
  )
}
