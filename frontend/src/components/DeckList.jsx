import { useEffect, useMemo, useState } from 'react'
import Button from './ui/Button'
import { fetchCardsByNames } from '../services/api'
import createIcon from '../assets/icons/create.png'
import importIcon from '../assets/icons/import.png'

const CARD_IMAGE_CACHE_KEY = 'mtg-card-image-cache-v2'
const COLOR_LABELS = {
  W: 'Branco',
  U: 'Azul',
  B: 'Preto',
  R: 'Vermelho',
  G: 'Verde',
}

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

function normalizeName(name) {
  return String(name || '').trim().toLowerCase()
}

function imageFor(cache, name) {
  const normalizedName = normalizeName(name)
  if (!normalizedName) return null
  return Object.entries(cache).find(([cardName]) => normalizeName(cardName) === normalizedName)?.[1] || null
}

function colorIdentity(deck) {
  const rawColors = Array.isArray(deck.colorIdentity)
    ? deck.colorIdentity
    : String(deck.colorIdentity || '').split('')
  const colors = rawColors.map((color) => String(color).toUpperCase()).filter((color) => COLOR_LABELS[color])
  return colors.length > 0 ? [...new Set(colors)] : ['C']
}

function totalCardsFor(deck) {
  return deck.mainDeckSize ?? deck.cardCount ?? deck.cards
    ?.reduce((sum, card) => sum + Number(card.quantity || 0), 0) ?? 0
}

function cardTotalLabel(totalCards) {
  return `${totalCards}/99 ${totalCards === 1 ? 'carta' : 'cartas'}`
}

export default function DeckList({
  decks = [],
  onEdit,
  onDelete,
  onConsult,
  onCopy,
  onLike,
  onCreate,
  onImport,
  actionsDisabled = false,
  actionHint = '',
  emptyTitle = 'Nenhum deck ainda',
  emptyDescription = 'Crie um deck Commander do zero ou importe uma lista de texto quando já tiver as 99 cartas separadas.',
  showCreateActions = true,
  showManageActions = true,
  copyLoadingId = null,
}) {
  const [cardImages, setCardImages] = useState(() => readImageCache())
  const commanderNames = useMemo(() => [...new Set(decks
    .map((deck) => String(deck.commander || '').trim())
    .filter(Boolean))], [decks])

  useEffect(() => {
    const missingNames = commanderNames.filter((name) => !imageFor(cardImages, name))
    if (missingNames.length === 0) return

    let cancelled = false
    async function loadCommanderImages() {
      const fetchedCards = await fetchCardsByNames(missingNames)
      if (cancelled) return

      const nextImages = {}
      fetchedCards.forEach((card) => {
        if (card?.name && card?.imageUrl) {
          nextImages[card.name] = card.imageUrl
        }
      })
      if (Object.keys(nextImages).length > 0) {
        setCardImages((previous) => {
          const merged = { ...previous, ...nextImages }
          writeImageCache(merged)
          return merged
        })
      }
    }

    loadCommanderImages()
    return () => {
      cancelled = true
    }
  }, [cardImages, commanderNames])

  if (!decks || decks.length === 0) {
    return (
      <div className="empty-state empty-state-hero">
        <p className="eyebrow">Biblioteca vazia</p>
        <h3>{emptyTitle}</h3>
        <p>{emptyDescription}</p>
        {actionsDisabled && actionHint && <p id="empty-action-hint" className="empty-action-hint">{actionHint}</p>}
        {showCreateActions && (
          <div className="actions-row" aria-describedby={actionsDisabled && actionHint ? 'empty-action-hint' : undefined}>
            <Button className="cta-primary" onClick={onCreate} disabled={actionsDisabled}>
              <img className="btn-icon" src={createIcon} alt="" aria-hidden="true" />
              Criar Deck
            </Button>
            <Button variant="secondary" onClick={onImport} disabled={actionsDisabled}>
              <img className="btn-icon" src={importIcon} alt="" aria-hidden="true" />
              Importar Deck
            </Button>
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="deck-list">
      {decks.map((deck) => {
        const totalCards = totalCardsFor(deck)
        const commanderImage = imageFor(cardImages, deck.commander)
        const colors = colorIdentity(deck)
        const isPublic = deck.visibility === 'public'
        const canCopy = Boolean(onCopy) && !deck.ownedByCurrentUser
        const canLike = isPublic && Boolean(onLike)
        const likeLabel = deck.likedByCurrentUser ? 'Remover like' : 'Curtir'

        return (
          <article key={deck.id} className={`deck-card deck-gallery-card ${isPublic ? 'public-deck-card' : ''}`}>
            <div className="deck-card-art" aria-hidden="true">
              {commanderImage ? (
                <img src={commanderImage} alt="" loading="lazy" referrerPolicy="no-referrer" />
              ) : (
                <span>{String(deck.commander || deck.name || 'Deck').trim().slice(0, 2).toUpperCase()}</span>
              )}
            </div>
            <div className="deck-card-body">
              <div className="deck-title">{deck.name}</div>
              <div className="deck-subtitle">{deck.commander}</div>
              <div className="deck-meta-row">
                <span className={`deck-count ${totalCards > 99 ? 'is-invalid' : ''}`}>{cardTotalLabel(totalCards)}</span>
                {deck.visibility && <span className="status-pill">{isPublic ? 'Público' : 'Privado'}</span>}
                {deck.ownedByCurrentUser && <span className="status-pill owned">Seu deck</span>}
                <span className="mana-pips" aria-label={colors[0] === 'C' ? 'Incolor' : `Cores: ${colors.map((color) => COLOR_LABELS[color]).join(', ')}`}>
                  {colors.map((color) => <i key={color} data-color={color}>{color}</i>)}
                </span>
              </div>
              {deck.author && <div className="deck-subtitle">por {deck.author}</div>}
              {isPublic && (
                <div className="deck-subtitle">
                  {Number(deck.likeCount || 0)} like{Number(deck.likeCount || 0) === 1 ? '' : 's'}
                  {deck.sourceType === 'external' && deck.externalSource ? ` - ${deck.externalSource}` : ''}
                </div>
              )}
            </div>
            <div className="actions-row deck-card-actions">
              {onConsult && <Button variant="secondary" onClick={() => onConsult(deck)}>Ver deck</Button>}
              {canLike && (
                <Button variant={deck.likedByCurrentUser ? 'secondary' : 'primary'} onClick={() => onLike(deck)}>
                  {likeLabel}
                </Button>
              )}
              {canCopy && (
                <Button onClick={() => onCopy(deck)} loading={copyLoadingId === deck.id}>
                  Copiar
                </Button>
              )}
              {showManageActions && (
                <>
                  <Button variant="secondary" onClick={() => onEdit && onEdit(deck)}>Editar</Button>
                  <Button variant="danger" onClick={() => onDelete && onDelete(deck)}>Excluir</Button>
                </>
              )}
            </div>
          </article>
        )
      })}
    </div>
  )
}
