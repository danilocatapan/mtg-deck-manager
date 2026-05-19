/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useMemo, useState } from 'react'
import CardSearch from './CardSearch'
import Button from './ui/Button'
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
    if (card?.name) {
      assign(normalizeCardName(card.name), card)
    }
  })
  requestedNames.forEach((name) => {
    const normalizedName = normalizeCardName(name)
    assign(normalizedName, fetchedByName.get(normalizedName))
  })

  return changed ? next : prev
}

export default function DeckForm({ initial = null, onCancel, onSave }) {
  const [name, setName] = useState('')
  const [commander, setCommander] = useState('')
  const [visibility, setVisibility] = useState('private')
  const [cards, setCards] = useState([])
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
  const [error, setError] = useState(null)
  const [savedMessage, setSavedMessage] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (initial) {
      setName(initial.name || '')
      setCommander(initial.commander || '')
      setVisibility(initial.visibility || 'private')
      setCards(initial.cards ? initial.cards.map((card) => ({ name: card.name, quantity: card.quantity })) : [])
    }
  }, [initial])

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

  const mainDeckTotal = useMemo(() => cards
    .reduce((sum, card) => sum + Number(card.quantity || 0), 0), [cards])
  const isOverLimit = mainDeckTotal > 99
  const isValid = Boolean(name.trim() && commander.trim() && mainDeckTotal > 0 && !isOverLimit)
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
    setCardDetails((prev) => ({ ...prev, [normalizeCardName(card.name)]: card }))
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

  function renderDeckRow(card) {
    return (
      <div key={card.name} className="deck-row">
        <div className="deck-row-card">
          <strong>{card.name}</strong>
          <span>{card.typeLine}</span>
        </div>
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
    )
  }

  function validate() {
    if (!name.trim() || !commander.trim()) {
      setError('Nome do deck e comandante são obrigatórios.')
      return false
    }
    if (mainDeckTotal === 0) {
      setError('Adicione pelo menos uma carta ao deck antes de salvar.')
      return false
    }
    if (isOverLimit) {
      setError(`Decks Commander podem ter até 99 cartas fora do comandante. A lista principal tem ${mainDeckTotal}.`)
      return false
    }
    setError(null)
    return true
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!validate()) return

    const payload = {
      name: name.trim(),
      commander: commander.trim(),
      visibility,
      cards: cards.map((card) => ({ name: card.name, quantity: card.quantity })),
    }
    setSavedMessage('Salvando deck...')
    setSaving(true)
    try {
      await onSave?.(payload)
      setSavedMessage(null)
    } catch (saveError) {
      setError(saveError.message || 'Falha ao salvar deck.')
      setSavedMessage(null)
    } finally {
      setSaving(false)
    }
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
          <label>
            Visibilidade
            <small>Decks públicos aparecem na vitrine e podem ser copiados por outros usuários.</small>
            <select value={visibility} onChange={(e) => setVisibility(e.target.value)}>
              <option value="private">Privado</option>
              <option value="public">Público</option>
            </select>
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
            <p>Busque uma carta e use Adicionar para colocá-la no deck.</p>
          </div>
        </div>
        <CardSearch onSelect={addCard} />
      </section>

      <section className="editor-section">
        <div className="section-heading">
          <div>
            <h3>Lista do deck - {mainDeckTotal} cartas</h3>
            <p>Use lista para edições rápidas ou imagens para reconhecer cartas visualmente.</p>
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
                onChange={(e) => setDeckNameFilter(e.target.value)}
                placeholder="Sol Ring, Forest..."
              />
            </label>
            <label>
              <span>Tipo</span>
              <select value={deckTypeFilter} onChange={(e) => setDeckTypeFilter(e.target.value)}>
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
                onChange={(e) => setGroupByType(e.target.checked)}
              />
              <span>Agrupar por tipo</span>
            </label>
            <div className="deck-list-summary" aria-live="polite">
              {loadingCardDetails ? 'Carregando tipos...' : `${cardTotalLabel(filteredTotal)} ${filteredTotal === 1 ? 'visível' : 'visíveis'}`}
            </div>
          </div>
        )}

        {cards.length === 0 ? (
          <div className="empty-inline">Nenhuma carta adicionada. Busque acima para começar.</div>
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
            <div className="deck-image-grid">
              {filteredCards.map((card) => (
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
                        <span>Imagem indisponível</span>
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
        <Button type="submit" disabled={!isValid || saving}>{saving ? 'Salvando...' : 'Salvar Deck'}</Button>
        <Button type="button" variant="secondary" onClick={onCancel}>Voltar</Button>
      </div>
    </form>
  )
}
