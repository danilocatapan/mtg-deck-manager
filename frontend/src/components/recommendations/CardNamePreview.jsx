import { useState } from 'react'
import { fetchCardsByNames } from '../../services/api'

const imageCache = new Map()

export default function CardNamePreview({ name, prefix = '' }) {
  const [preview, setPreview] = useState(() => imageCache.get(normalizeName(name)) || null)
  const [loading, setLoading] = useState(false)

  // Lazy-load the card image only when the user hovers or focuses the name.
  async function loadPreview() {
    const key = normalizeName(name)
    if (!key || preview || loading) return

    setLoading(true)
    const [card] = await fetchCardsByNames([name])
    const nextPreview = card?.imageUrl ? { name: card.name || name, imageUrl: card.imageUrl } : { name, imageUrl: null }
    imageCache.set(key, nextPreview)
    setPreview(nextPreview)
    setLoading(false)
  }

  return (
    <span className="card-name-preview" onPointerEnter={loadPreview} onFocus={loadPreview}>
      <button type="button" className="card-name-trigger" aria-label={`Ver imagem de ${name}`}>
        {prefix}{name}
      </button>
      <span className="card-name-popover" role="tooltip">
        {preview?.imageUrl ? (
          <img src={preview.imageUrl} alt={preview.name} loading="lazy" referrerPolicy="no-referrer" />
        ) : (
          <span className="card-name-popover-empty">{loading ? 'Carregando imagem...' : 'Imagem indisponível'}</span>
        )}
      </span>
    </span>
  )
}

function normalizeName(name) {
  return String(name || '').trim().toLowerCase()
}
