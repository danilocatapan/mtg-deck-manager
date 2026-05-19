import { useEffect, useMemo, useState } from 'react'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
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

function imageFor(cache, name) {
  const normalizedName = normalizeCardName(name)
  if (!normalizedName) return null
  return Object.entries(cache).find(([cardName]) => normalizeCardName(cardName) === normalizedName)?.[1] || null
}

export default function DeckConsultPage({ deck, isAuthenticated = false, onBack, onCopy, onLoginRequired }) {
  const [cardImages, setCardImages] = useState(() => readImageCache())
  const [requestedImages, setRequestedImages] = useState({})
  const [loadingImages, setLoadingImages] = useState(false)

  const cards = useMemo(() => deck?.cards || [], [deck?.cards])
  const totalCards = useMemo(() => cards.reduce((sum, card) => sum + Number(card.quantity || 0), 0), [cards])
  const displayTotal = deck?.mainDeckSize ?? totalCards
  const namesToLoad = useMemo(() => {
    const names = [deck?.commander, ...cards.map((card) => card.name)]
      .map((name) => String(name || '').trim())
      .filter(Boolean)
    return [...new Set(names)].filter((name) => !imageFor(cardImages, name) && !requestedImages[normalizeCardName(name)])
  }, [cardImages, cards, deck?.commander, requestedImages])

  useEffect(() => {
    if (namesToLoad.length === 0) return
    let cancelled = false

    async function loadImages() {
      setLoadingImages(true)
      const fetchedCards = await fetchCardsByNames(namesToLoad)
      const nextImages = {}
      fetchedCards.forEach((card) => {
        if (card?.name && card?.imageUrl) {
          nextImages[card.name] = card.imageUrl
        }
      })
      if (!cancelled) {
        setRequestedImages((previous) => ({
          ...previous,
          ...namesToLoad.reduce((acc, name) => ({ ...acc, [normalizeCardName(name)]: true }), {}),
        }))
        if (Object.keys(nextImages).length > 0) {
          setCardImages((previous) => {
            const merged = { ...previous, ...nextImages }
            writeImageCache(merged)
            return merged
          })
        }
        setLoadingImages(false)
      }
    }

    loadImages()
    return () => {
      cancelled = true
    }
  }, [namesToLoad])

  return (
    <main>
      <section className="zone zone-command page-heading deck-editor-heading">
        <div>
          <p className="eyebrow">Consulta</p>
          <h1>{deck?.name || 'Deck'}</h1>
          <p className="page-description">
            {deck?.commander} - {displayTotal}/99 cartas - {deck?.visibility === 'public' ? 'Público' : 'Privado'}
            {deck?.author ? ` - por ${deck.author}` : ''}
          </p>
        </div>
        <div className="actions-row">
          <Button onClick={() => isAuthenticated ? onCopy?.(deck) : onLoginRequired?.()}>
            {isAuthenticated ? 'Copiar para minha biblioteca' : 'Entrar para copiar'}
          </Button>
          <Button variant="secondary" onClick={onBack}>Voltar aos Decks</Button>
        </div>
      </section>

      <Card className="zone zone-library">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Somente leitura</p>
            <h2>Lista do deck</h2>
            <p>Este modo permite consultar a lista sem editar, excluir ou aplicar recomendações. Cópias entram como privadas na sua biblioteca.</p>
          </div>
        </div>

        {loadingImages && <div className="deck-image-status">Carregando imagens das cartas...</div>}

        <div className="deck-image-grid consult-grid">
          {cards.map((card) => {
            const imageUrl = imageFor(cardImages, card.name)
            return (
              <article key={card.name} className="deck-image-card">
                <div className="card-art-frame">
                  {imageUrl ? (
                    <img src={imageUrl} alt={card.name} loading="lazy" referrerPolicy="no-referrer" />
                  ) : (
                    <div className="card-art-placeholder">
                      <strong>{card.name}</strong>
                      <span>Imagem indisponível</span>
                    </div>
                  )}
                  <span className="card-quantity-badge">{card.quantity}x</span>
                </div>
              </article>
            )
          })}
        </div>
      </Card>
    </main>
  )
}
