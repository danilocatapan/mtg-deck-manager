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

function normalizeCardName(name) {
  return String(name || '').trim().toLowerCase()
}

function imageForName(cache, name) {
  const normalizedName = normalizeCardName(name)
  if (!normalizedName) return null
  return Object.entries(cache).find(([cardName]) => normalizeCardName(cardName) === normalizedName)?.[1] || null
}

export default function DeckForm({ initial = null, onCancel, onSave }) {
  const [name, setName] = useState('')
  const [commander, setCommander] = useState('')
  const [cards, setCards] = useState([])
  const [deckView, setDeckView] = useState('list')
  const [cardImages, setCardImages] = useState(() => readImageCache())
  const [unavailableImages, setUnavailableImages] = useState({})
  const [requestedImages, setRequestedImages] = useState({})
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
    const missingCards = cards.filter((card) => (
      card.name
      && !cardImages[card.name]
      && !unavailableImages[card.name]
      && !requestedImages[card.name]
    ))
    if (missingCards.length === 0) return

    let cancelled = false
    async function loadImages() {
      setLoadingImages(true)
      const nextImages = {}
      const fetchedCards = await fetchCardsByNames(missingCards.map((card) => card.name))
      const fetchedByName = new Map(fetchedCards.map((card) => [card.name?.toLowerCase(), card]))
      const unavailable = {}
      for (const deckCard of missingCards) {
        const fetched = fetchedByName.get(deckCard.name.toLowerCase())
        if (fetched?.imageUrl) {
          nextImages[deckCard.name] = fetched.imageUrl
        } else if (fetched) {
          unavailable[deckCard.name] = true
        }
      }
      if (!cancelled) {
        setRequestedImages((prev) => ({
          ...prev,
          ...missingCards.reduce((acc, card) => ({ ...acc, [card.name]: true }), {}),
        }))
        if (Object.keys(nextImages).length > 0) {
          setCardImages((prev) => {
            const merged = { ...prev, ...nextImages }
            writeImageCache(merged)
            return merged
          })
        }
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
  }, [deckView, cards, cardImages, unavailableImages, requestedImages])

  const mainDeckTotal = useMemo(() => cards
    .reduce((sum, card) => sum + Number(card.quantity || 0), 0), [cards])
  const missingImageCount = useMemo(() => cards.filter((card) => card.name && !cardImages[card.name]).length, [cards, cardImages])
  const isOverLimit = mainDeckTotal > 99
  const isValid = Boolean(name.trim() && commander.trim() && mainDeckTotal > 0 && !isOverLimit)
  const commanderName = commander.trim()
  const commanderImageUrl = useMemo(() => imageForName(cardImages, commanderName), [cardImages, commanderName])
  const commanderInitials = commander
    .split(/[,\s]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || 'C'

  useEffect(() => {
    if (!commanderName || commanderName.length < 3) return
    if (commanderImageUrl || unavailableImages[normalizeCardName(commanderName)]) return

    let cancelled = false
    const timeoutId = window.setTimeout(async () => {
      const fetchedCards = await fetchCardsByNames([commanderName])
      const fetched = fetchedCards.find((card) => normalizeCardName(card.name) === normalizeCardName(commanderName)) || fetchedCards[0]

      if (cancelled) return

      if (fetched?.imageUrl) {
        setCardImages((prev) => {
          const merged = { ...prev, [commanderName]: fetched.imageUrl, [fetched.name]: fetched.imageUrl }
          writeImageCache(merged)
          return merged
        })
      } else {
        setUnavailableImages((prev) => ({ ...prev, [normalizeCardName(commanderName)]: true }))
      }
    }, 350)

    return () => {
      cancelled = true
      window.clearTimeout(timeoutId)
    }
  }, [commanderName, commanderImageUrl, unavailableImages])

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
    setUnavailableImages((prev) => ({ ...prev, [cardName]: true, [normalizeCardName(cardName)]: true }))
    setRequestedImages((prev) => ({ ...prev, [cardName]: true }))
    setCardImages((prev) => {
      const merged = { ...prev }
      Object.keys(merged)
        .filter((name) => normalizeCardName(name) === normalizeCardName(cardName))
        .forEach((name) => delete merged[name])
      writeImageCache(merged)
      return merged
    })
  }

  function retryImages() {
    setUnavailableImages({})
    setRequestedImages({})
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
      setError('Nome do deck e comandante sao obrigatorios.')
      return false
    }
    if (mainDeckTotal === 0) {
      setError('Adicione pelo menos uma carta ao deck antes de salvar.')
      return false
    }
    if (isOverLimit) {
      setError(`Decks Commander podem ter ate 99 cartas fora do comandante. A lista principal tem ${mainDeckTotal}.`)
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
    setSavedMessage('Salvando deck...')
    onSave && onSave(payload)
  }

  return (
    <form onSubmit={handleSubmit} className="deck-editor-form">
      <section className="deck-identity-panel">
        <div className="form-grid">
          <label>
            Nome do deck
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Gruul Revels" />
          </label>

          <label>
            Comandante
            <input value={commander} onChange={(e) => setCommander(e.target.value)} placeholder="Xenagos, God of Revels" />
          </label>
        </div>

        <div className="commander-card">
          <div className={commanderImageUrl ? 'commander-art' : 'commander-sigil'} aria-hidden="true">
            {commanderImageUrl ? (
              <img src={commanderImageUrl} alt="" loading="lazy" referrerPolicy="no-referrer" onError={() => markImageUnavailable(commanderName)} />
            ) : commanderInitials}
          </div>
          <div className="commander-details">
            <p className="eyebrow">Comandante</p>
            <h2>{commander.trim() || 'Escolha seu comandante'}</h2>
          </div>
        </div>

        <div className="deck-health">
          <div>
            <strong className={isOverLimit ? 'is-invalid' : ''}>{mainDeckTotal}/99</strong>
            <span> cartas no deck</span>
          </div>
          <div className={isValid ? 'status-pill ready' : 'status-pill'}>
            {isValid ? 'Pronto para salvar' : isOverLimit ? 'Acima do limite Commander' : 'Faltam nome, comandante e cartas'}
          </div>
        </div>
      </section>

      <section className="editor-section compact-add">
        <div className="section-heading">
          <div>
            <h3>Adicionar cartas</h3>
            <p>Busque uma carta e use Adicionar para coloca-la no deck.</p>
          </div>
        </div>
        <CardSearch onSelect={addCard} />
      </section>

      <section className="editor-section">
        <div className="section-heading">
          <div>
            <h3>Lista do deck - {mainDeckTotal} cartas</h3>
            <p>Use lista para edicoes rapidas ou imagens para reconhecer cartas visualmente.</p>
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
          <div className="empty-inline">Nenhuma carta adicionada. Busque acima para comecar.</div>
        ) : deckView === 'list' ? (
          <div className="deck-card-list-scroll">
            <div className="deck-table">
              {cards.map((card) => (
                <div key={card.name} className="deck-row">
                  <strong>{card.name}</strong>
                  <input
                    aria-label={`Quantidade de ${card.name}`}
                    type="number"
                    value={card.quantity}
                    min={1}
                    onChange={(e) => changeQuantity(card.name, parseInt(e.target.value || '1', 10))}
                  />
                  <Button type="button" variant="danger" onClick={() => removeCard(card.name)}>
                    Remover
                  </Button>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <>
            {(loadingImages || missingImageCount > 0) && (
              <div className="deck-image-status">
                {loadingImages ? <span>Carregando imagens das cartas...</span> : <span>{missingImageCount} imagem(ns) pendente(s).</span>}
                {missingImageCount > 0 && (
                  <Button type="button" variant="secondary" onClick={retryImages}>
                    Recarregar imagens
                  </Button>
                )}
              </div>
            )}
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
                        <span>Imagem indisponivel</span>
                      </div>
                    )}
                    <span className="card-quantity-badge">{card.quantity}x</span>
                  </div>
                  <div className="image-card-actions">
                    <input
                      aria-label={`Quantidade de ${card.name}`}
                      type="number"
                      value={card.quantity}
                      min={1}
                      onChange={(e) => changeQuantity(card.name, parseInt(e.target.value || '1', 10))}
                    />
                    <Button type="button" variant="secondary" className="image-remove-button" aria-label={`Remover ${card.name}`} onClick={() => removeCard(card.name)}>
                      Remover
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
        <Button type="submit" disabled={!isValid}>Salvar Deck</Button>
        <Button type="button" variant="secondary" onClick={onCancel}>Voltar</Button>
      </div>
    </form>
  )
}
