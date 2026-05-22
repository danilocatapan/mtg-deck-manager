/* eslint-disable react-hooks/set-state-in-effect */
import { useEffect, useMemo, useState } from 'react'
import CardSearch from './CardSearch'
import DeckCardListView from './DeckCardListView'
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
  const [visibility, setVisibility] = useState('private')
  const [cards, setCards] = useState([])
  const [cardImages, setCardImages] = useState(() => readImageCache())
  const [unavailableImages, setUnavailableImages] = useState({})
  const [error, setError] = useState(null)
  const [savedMessage, setSavedMessage] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (initial) {
      setName(initial.name || '')
      setCommander(initial.commander || '')
      setVisibility(initial.visibility || 'private')
      setCards(initial.cards ? initial.cards.map((card) => ({ ...card, name: card.name, quantity: card.quantity })) : [])
    }
  }, [initial])

  const mainDeckTotal = useMemo(() => cards
    .reduce((sum, card) => sum + Number(card.quantity || 0), 0), [cards])
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
      return [...prev, { ...card, name: card.name, quantity: 1 }]
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
    setCardImages((prev) => {
      const merged = { ...prev }
      Object.keys(merged)
        .filter((cachedName) => normalizeCardName(cachedName) === normalizeCardName(cardName))
        .forEach((cachedName) => delete merged[cachedName])
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

  async function handleSubmit(e) {
    e.preventDefault()
    if (!validate()) return

    const payload = {
      name: name.trim(),
      commander: commander.trim(),
      visibility,
      cards: cards.map((card) => ({
        name: card.name,
        quantity: card.quantity,
        scryfallId: card.scryfallId,
        setCode: card.setCode,
        setName: card.setName,
        collectorNumber: card.collectorNumber,
        finish: card.finish,
        imageUrl: card.imageUrl,
      })),
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
            <small>Decks publicos aparecem na vitrine e podem ser copiados por outros usuarios.</small>
            <select value={visibility} onChange={(e) => setVisibility(e.target.value)}>
              <option value="private">Privado</option>
              <option value="public">Publico</option>
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
            <p>Busque uma carta e use Adicionar para coloca-la no deck.</p>
          </div>
        </div>
        <CardSearch onSelect={addCard} />
      </section>

      <DeckCardListView
        cards={cards}
        editable
        title="Lista do deck"
        description="Use lista para edicoes rapidas ou imagens para reconhecer cartas visualmente."
        emptyMessage="Nenhuma carta adicionada. Busque acima para comecar."
        onQuantityChange={changeQuantity}
        onRemove={removeCard}
      />

      {error && <div className="status error">{error}</div>}
      {savedMessage && <div className="status">{savedMessage}</div>}

      <div className="form-actions">
        <Button type="submit" disabled={!isValid || saving}>{saving ? 'Salvando...' : 'Salvar Deck'}</Button>
        <Button type="button" variant="secondary" onClick={onCancel}>Voltar</Button>
      </div>
    </form>
  )
}
