import { useEffect, useMemo, useState } from 'react'
import Button from './ui/Button'
import CardNamePreview from './CardNamePreview'
import { fetchCardsByNames } from '../services/api'

const CARD_IMAGE_CACHE_KEY = 'mtg-card-image-cache-v2'
const CARD_TYPE_GROUPS = [
  { key: 'land', label: 'Terrenos', matcher: 'land' },
  { key: 'creature', label: 'Criaturas', matcher: 'creature' },
  { key: 'artifact', label: 'Artefatos', matcher: 'artifact' },
  { key: 'enchantment', label: 'Encantamentos', matcher: 'enchantment' },
  { key: 'planeswalker', label: 'Planeswalkers', matcher: 'planeswalker' },
  { key: 'instant', label: 'Instantaneas', matcher: 'instant' },
  { key: 'sorcery', label: 'Feiticos', matcher: 'sorcery' },
  { key: 'battle', label: 'Batalhas', matcher: 'battle' },
  { key: 'other', label: 'Outros' },
]

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

function typeGroupFor(typeLine) {
  const normalizedType = normalizeCardName(typeLine)
  if (!normalizedType) return CARD_TYPE_GROUPS[CARD_TYPE_GROUPS.length - 1]
  return CARD_TYPE_GROUPS.find((group) => group.matcher && normalizedType.includes(group.matcher)) || CARD_TYPE_GROUPS[CARD_TYPE_GROUPS.length - 1]
}

function cardTotalLabel(quantity) {
  return `${quantity} ${quantity === 1 ? 'carta' : 'cartas'}`
}

function mergeCardDetails(prev, fetchedCards, requestedNames = []) {
  const next = { ...prev }
  let changed = false
  const fetchedByName = new Map(fetchedCards
    .filter((card) => card?.name)
    .map((card) => [normalizeCardName(card.name), card]))

  function assign(key, card) {
    if (!key || !card) return
    const current = next[key]
    if (current?.name === card.name && current?.typeLine === card.typeLine && current?.imageUrl === card.imageUrl) {
      return
    }
    next[key] = card
    changed = true
  }

  fetchedCards.forEach((card) => {
    if (card?.name) assign(normalizeCardName(card.name), card)
  })
  requestedNames.forEach((name) => assign(normalizeCardName(name), fetchedByName.get(normalizeCardName(name))))

  return changed ? next : prev
}

export default function DeckCardListView({
  cards = [],
  editable = false,
  title = 'Lista do deck',
  description = '',
  emptyMessage = 'Nenhuma carta adicionada.',
  onQuantityChange,
  onRemove,
}) {
  const [deckNameFilter, setDeckNameFilter] = useState('')
  const [deckTypeFilter, setDeckTypeFilter] = useState('all')
  const [groupByType, setGroupByType] = useState(true)
  const [deckView, setDeckView] = useState('list')
  const [cardDetails, setCardDetails] = useState({})
  const [cardImages, setCardImages] = useState(() => readImageCache())
  const [unavailableImages, setUnavailableImages] = useState({})
  const [requestedImages, setRequestedImages] = useState({})
  const [loadingImages, setLoadingImages] = useState(false)
  const [loadingCardDetails, setLoadingCardDetails] = useState(false)

  useEffect(() => {
    const missingNames = cards
      .map((card) => card.name)
      .filter((cardName) => cardName && !cardDetails[normalizeCardName(cardName)])
    if (missingNames.length === 0) return

    let cancelled = false
    async function loadCardDetails() {
      setLoadingCardDetails(true)
      const fetchedCards = await fetchCardsByNames(missingNames)
      if (cancelled) return

      if (fetchedCards.length > 0) {
        setCardDetails((prev) => mergeCardDetails(prev, fetchedCards, missingNames))
      }
      setLoadingCardDetails(false)
    }

    loadCardDetails()
    return () => {
      cancelled = true
    }
  }, [cards, cardDetails])

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
        if (fetchedCards.length > 0) {
          setCardDetails((prev) => mergeCardDetails(prev, fetchedCards, missingCards.map((card) => card.name)))
        }
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

  const typedCards = useMemo(() => cards.map((card) => {
    const details = cardDetails[normalizeCardName(card.name)]
    const typeGroup = typeGroupFor(details?.typeLine)
    return {
      ...card,
      typeLine: details?.typeLine || 'Tipo pendente',
      typeGroupKey: typeGroup.key,
      typeGroupLabel: typeGroup.label,
    }
  }), [cards, cardDetails])

  const availableTypeGroups = useMemo(() => CARD_TYPE_GROUPS
    .map((group) => ({
      ...group,
      count: typedCards
        .filter((card) => card.typeGroupKey === group.key)
        .reduce((sum, card) => sum + Number(card.quantity || 0), 0),
    }))
    .filter((group) => group.count > 0), [typedCards])

  const filteredCards = useMemo(() => {
    const normalizedFilter = normalizeCardName(deckNameFilter)
    return typedCards.filter((card) => {
      const matchesName = !normalizedFilter || normalizeCardName(card.name).includes(normalizedFilter)
      const matchesType = deckTypeFilter === 'all' || card.typeGroupKey === deckTypeFilter
      return matchesName && matchesType
    })
  }, [deckNameFilter, deckTypeFilter, typedCards])

  const filteredTotal = useMemo(() => filteredCards
    .reduce((sum, card) => sum + Number(card.quantity || 0), 0), [filteredCards])

  const groupedCards = useMemo(() => CARD_TYPE_GROUPS
    .map((group) => ({
      ...group,
      cards: filteredCards.filter((card) => card.typeGroupKey === group.key),
    }))
    .filter((group) => group.cards.length > 0), [filteredCards])

  const missingImageCount = useMemo(() => filteredCards.filter((card) => card.name && !cardImages[card.name]).length, [filteredCards, cardImages])
  const totalCards = useMemo(() => cards.reduce((sum, card) => sum + Number(card.quantity || 0), 0), [cards])

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

  function imageForCard(card) {
    const details = cardDetails[normalizeCardName(card.name)]
    return details?.imageUrl || imageForName(cardImages, card.name)
  }

  function renderDeckRow(card) {
    return (
      <div key={card.name} className={editable ? 'deck-row' : 'deck-row deck-row-readonly'}>
        <div className="deck-row-card">
          <strong><CardNamePreview name={card.name} imageUrl={imageForCard(card)} /></strong>
          <span>{card.typeLine}</span>
        </div>
        {editable ? (
          <>
            <input
              aria-label={`Quantidade de ${card.name}`}
              type="number"
              value={card.quantity}
              min={1}
              onChange={(event) => onQuantityChange?.(card.name, parseInt(event.target.value || '1', 10))}
            />
            <Button type="button" variant="danger" onClick={() => onRemove?.(card.name)}>
              Remover
            </Button>
          </>
        ) : (
          <span className="deck-row-quantity">{card.quantity}x</span>
        )}
      </div>
    )
  }

  return (
    <section className="editor-section">
      <div className="section-heading">
        <div>
          <h3>{title} - {totalCards} cartas</h3>
          {description && <p>{description}</p>}
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

      {cards.length > 0 && (
        <div className="deck-list-toolbar">
          <label className="deck-list-search">
            <span>Filtrar por nome</span>
            <input
              value={deckNameFilter}
              onChange={(event) => setDeckNameFilter(event.target.value)}
              placeholder="Sol Ring, Forest..."
            />
          </label>
          <label>
            <span>Tipo</span>
            <select value={deckTypeFilter} onChange={(event) => setDeckTypeFilter(event.target.value)}>
              <option value="all">Todos os tipos</option>
              {availableTypeGroups.map((group) => (
                <option key={group.key} value={group.key}>{group.label} ({group.count})</option>
              ))}
            </select>
          </label>
          <label className="deck-list-checkbox">
            <input
              type="checkbox"
              checked={groupByType}
              onChange={(event) => setGroupByType(event.target.checked)}
            />
            <span>Agrupar por tipo</span>
          </label>
          <div className="deck-list-summary" aria-live="polite">
            {loadingCardDetails ? 'Carregando tipos...' : `${cardTotalLabel(filteredTotal)} ${filteredTotal === 1 ? 'visivel' : 'visiveis'}`}
          </div>
        </div>
      )}

      {cards.length === 0 ? (
        <div className="empty-inline">{emptyMessage}</div>
      ) : filteredCards.length === 0 ? (
        <div className="empty-inline">Nenhuma carta corresponde aos filtros atuais.</div>
      ) : deckView === 'list' ? (
        <div className="deck-card-list-scroll">
          {groupByType ? (
            <div className="deck-type-groups">
              {groupedCards.map((group) => {
                const groupTotal = group.cards.reduce((sum, card) => sum + Number(card.quantity || 0), 0)
                return (
                  <section key={group.key} className="deck-type-group">
                    <div className="deck-type-heading">
                      <h4>{group.label}</h4>
                      <span>{cardTotalLabel(groupTotal)}</span>
                    </div>
                    <div className="deck-table">
                      {group.cards.map(renderDeckRow)}
                    </div>
                  </section>
                )
              })}
            </div>
          ) : (
            <div className="deck-table">
              {filteredCards.map(renderDeckRow)}
            </div>
          )}
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
          <div className="deck-image-grid-scroll">
            <div className="deck-image-grid">
              {filteredCards.map((card) => (
                <article key={card.name} className="deck-image-card">
                  <div className="card-art-frame">
                    {imageForCard(card) ? (
                      <img
                        src={imageForCard(card)}
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
                  {editable && (
                    <div className="image-card-actions">
                      <input
                        aria-label={`Quantidade de ${card.name}`}
                        type="number"
                        value={card.quantity}
                        min={1}
                        onChange={(event) => onQuantityChange?.(card.name, parseInt(event.target.value || '1', 10))}
                      />
                      <Button type="button" variant="secondary" className="image-remove-button" aria-label={`Remover ${card.name}`} onClick={() => onRemove?.(card.name)}>
                        Remover
                      </Button>
                    </div>
                  )}
                </article>
              ))}
            </div>
          </div>
        </>
      )}
    </section>
  )
}
