/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useMemo, useState } from 'react'
import CardSearch from './CardSearch'
import Button from './ui/Button'
import { fetchCardsByNames } from '../services/api'

const CARD_IMAGE_CACHE_KEY = 'mtg-card-image-cache-v2'

function readImageCache() {
  try {
    const cache = JSON.parse(window.localStorage.getItem(CARD_IMAGE_CACHE_KEY) || '{}')
    return Object.fromEntries(
      Object.entries(cache).filter(([, imageUrl]) => typeof imageUrl === 'string' && imageUrl.length > 0),
    )
  } catch {
    return {}
  }
}

function writeImageCache(cache) {
  try {
    window.localStorage.setItem(CARD_IMAGE_CACHE_KEY, JSON.stringify(cache))
  } catch {
    // Best-effort cache only.
  }
}

export default function DeckForm({ initial = null, onCancel, onSave }) {
  const [name, setName] = useState('')
  const [commander, setCommander] = useState('')
  const [cards, setCards] = useState([])
  const [deckView, setDeckView] = useState('list')
  const [cardImages, setCardImages] = useState(() => readImageCache())
  const [unavailableImages, setUnavailableImages] = useState({})
  const [loadingImages, setLoadingImages] = useState(false)
  const [error, setError] = useState(null)
  const [savedMessage, setSavedMessage] = useState(null)

  useEffect(() => {
    if (initial) {
      setName(initial.name || '')
      setCommander(initial.commander || '')
      setCards(initial.cards ? initial.cards.map((card) => ({ name: card.name, quantity: card.quantity })) : [])
    }
  }, [initial])

  useEffect(() => {
    if (deckView !== 'images' || cards.length === 0) return
    const missingCards = cards.filter((card) => card.name && !cardImages[card.name] && !unavailableImages[card.name])
    if (missingCards.length === 0) return

    let cancelled = false
    async function loadImages() {
      setLoadingImages(true)
      const nextImages = {}
      const fetchedCards = await fetchCardsByNames(missingCards.map((card) => card.name))
      const fetchedByName = new Map(fetchedCards.map((card) => [card.name?.toLowerCase(), card]))
      for (const deckCard of missingCards) {
        const fetched = fetchedByName.get(deckCard.name.toLowerCase())
        if (fetched?.imageUrl) {
          nextImages[deckCard.name] = fetched.imageUrl
        }
      }
      if (!cancelled) {
        if (Object.keys(nextImages).length > 0) {
          setCardImages((prev) => {
            const merged = { ...prev, ...nextImages }
            writeImageCache(merged)
            return merged
          })
        }
        const unavailable = missingCards
          .filter((card) => !nextImages[card.name])
          .reduce((acc, card) => ({ ...acc, [card.name]: true }), {})
        if (Object.keys(unavailable).length > 0) {
          setUnavailableImages((prev) => ({ ...prev, ...unavailable }))
        }
        setLoadingImages(false)
      }
    }
    loadImages()
    return () => {
      cancelled = true
    }
  }, [deckView, cards, cardImages, unavailableImages])

  const totalCards = useMemo(() => cards.reduce((sum, card) => sum + Number(card.quantity || 0), 0), [cards])
  const isOverLimit = totalCards > 99
  const isValid = Boolean(name.trim() && commander.trim() && cards.length > 0 && !isOverLimit)
  const commanderInitials = commander
    .split(/[,\s]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || 'C'

  function addCard(card) {
    setSavedMessage(null)
    setCards((prev) => {
      const found = prev.find((item) => item.name === card.name)
      if (found) return prev.map((item) => (item.name === card.name ? { ...item, quantity: item.quantity + 1 } : item))
      return [...prev, { name: card.name, quantity: 1 }]
    })
    if (card.imageUrl) {
      setCardImages((prev) => {
        const merged = { ...prev, [card.name]: card.imageUrl }
        writeImageCache(merged)
        return merged
      })
    }
  }

  function markImageUnavailable(cardName) {
    setUnavailableImages((prev) => ({ ...prev, [cardName]: true }))
    setCardImages((prev) => {
      const merged = { ...prev }
      delete merged[cardName]
      writeImageCache(merged)
      return merged
    })
  }

  function removeCard(nameToRemove) {
    setSavedMessage(null)
    setCards((prev) => prev.filter((card) => card.name !== nameToRemove))
  }

  function changeQuantity(cardName, qty) {
    setSavedMessage(null)
    setCards((prev) => prev.map((card) => (card.name === cardName ? { ...card, quantity: Math.max(1, qty) } : card)))
  }

  function validate() {
    if (!name.trim() || !commander.trim()) {
      setError('Deck name and commander are required.')
      return false
    }
    if (cards.length === 0) {
      setError('Add at least one card before saving.')
      return false
    }
    if (isOverLimit) {
      setError(`Commander decks can have up to 99 cards outside the commander. Current list has ${totalCards}.`)
      return false
    }
    setError(null)
    return true
  }

  function handleSubmit(e) {
    e.preventDefault()
    if (!validate()) return

    const payload = {
      name: name.trim(),
      commander: commander.trim(),
      cards: cards.map((card) => ({ name: card.name, quantity: card.quantity })),
    }
    setSavedMessage('Saving deck...')
    onSave && onSave(payload)
  }

  return (
    <form onSubmit={handleSubmit} className="deck-editor-form">
      <section className="commander-card">
        <div className="commander-sigil" aria-hidden="true">{commanderInitials}</div>
        <div className="commander-details">
          <p className="eyebrow">Commander Identity</p>
          <h2>{commander.trim() || 'Choose your commander'}</h2>
          <div className="commander-meta">
            <span>{commander.trim() ? 'Commander legal check pending' : 'Name defines color identity and recommendations'}</span>
            <span>{name.trim() || 'Untitled deck'}</span>
          </div>
        </div>
      </section>

      <div className="form-grid">
        <label>
          Deck name
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Gruul Revels" />
        </label>

        <label>
          Commander
          <input value={commander} onChange={(e) => setCommander(e.target.value)} placeholder="Xenagos, God of Revels" />
        </label>
      </div>

      <div className="deck-health">
        <div>
          <strong className={isOverLimit ? 'is-invalid' : ''}>{totalCards}/99</strong>
          <span> cards in deck</span>
        </div>
        <div className={isValid ? 'status-pill ready' : 'status-pill'}>
          {isValid ? 'Ready to save' : isOverLimit ? 'Over Commander limit' : 'Needs name, commander and cards'}
        </div>
      </div>

      <section className="editor-section">
        <div className="section-heading">
          <div>
            <h3>Add cards</h3>
            <p>Search a card and use Add to place it directly into this deck.</p>
          </div>
        </div>
        <CardSearch onSelect={addCard} />
      </section>

      <section className="editor-section">
        <div className="section-heading">
          <div>
            <h3>Deck list</h3>
            <p>Use list mode for fast edits or image mode to recognize cards visually.</p>
          </div>
          <div className="view-toggle" aria-label="Deck display mode">
            <button type="button" className={deckView === 'list' ? 'active' : ''} onClick={() => setDeckView('list')}>
              Lista
            </button>
            <button type="button" className={deckView === 'images' ? 'active' : ''} onClick={() => setDeckView('images')}>
              Imagens
            </button>
          </div>
        </div>

        {cards.length === 0 ? (
          <div className="empty-inline">No cards added yet. Search above to start building.</div>
        ) : deckView === 'list' ? (
          <div className="deck-table">
            {cards.map((card) => (
              <div key={card.name} className="deck-row">
                <strong>{card.name}</strong>
                <input
                  aria-label={`Quantity for ${card.name}`}
                  type="number"
                  value={card.quantity}
                  min={1}
                  onChange={(e) => changeQuantity(card.name, parseInt(e.target.value || '1', 10))}
                />
                <Button type="button" variant="danger" onClick={() => removeCard(card.name)}>
                  Remove
                </Button>
              </div>
            ))}
          </div>
        ) : (
          <>
            {loadingImages && <div className="loading">Loading card images...</div>}
            <div className="deck-image-grid">
              {cards.map((card) => (
                <article key={card.name} className="deck-image-card">
                  <div className="card-art-frame">
                    {cardImages[card.name] ? (
                      <img
                        src={cardImages[card.name]}
                        alt={card.name}
                        loading="lazy"
                        referrerPolicy="no-referrer"
                        onError={() => markImageUnavailable(card.name)}
                      />
                    ) : (
                      <div className="card-art-placeholder">
                        <strong>{card.name}</strong>
                        <span>Image unavailable</span>
                      </div>
                    )}
                    <span className="card-quantity-badge">{card.quantity}x</span>
                  </div>
                  <div className="image-card-actions">
                    <input
                      aria-label={`Quantity for ${card.name}`}
                      type="number"
                      value={card.quantity}
                      min={1}
                      onChange={(e) => changeQuantity(card.name, parseInt(e.target.value || '1', 10))}
                    />
                    <Button type="button" variant="danger" onClick={() => removeCard(card.name)}>
                      Remove
                    </Button>
                  </div>
                </article>
              ))}
            </div>
          </>
        )}
      </section>

      {error && <div className="status error">{error}</div>}
      {savedMessage && <div className="status">{savedMessage}</div>}

      <div className="form-actions">
        <Button type="submit" disabled={!isValid}>Save Deck</Button>
        <Button type="button" variant="secondary" onClick={onCancel}>Back</Button>
      </div>
    </form>
  )
}
