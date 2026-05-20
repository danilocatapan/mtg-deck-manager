import { useState } from 'react'
import { fetchCardsByNames } from '../services/api'

const imageCache = new Map()

export default function CardNamePreview({ name, prefix = '', imageUrl = null }) {
  const [loadedPreview, setLoadedPreview] = useState(null)
  const [loading, setLoading] = useState(false)
  const key = normalizeName(name)
  const propPreview = imageUrl ? { name, imageUrl } : null
  const cachedPreview = key ? imageCache.get(key) || null : null
  const preview = propPreview || (loadedPreview?.key === key ? loadedPreview.preview : null) || cachedPreview

  async function loadPreview() {
    if (!key || loading) return
    if (imageUrl) {
      const nextPreview = { name, imageUrl }
      imageCache.set(key, nextPreview)
      setLoadedPreview({ key, preview: nextPreview })
      return
    }
    if (preview) return

    setLoading(true)
    const [card] = await fetchCardsByNames([name])
    const nextPreview = card?.imageUrl ? { name: card.name || name, imageUrl: card.imageUrl } : { name, imageUrl: null }
    if (nextPreview.imageUrl) {
      imageCache.set(key, nextPreview)
    } else {
      imageCache.delete(key)
    }
    setLoadedPreview({ key, preview: nextPreview })
    setLoading(false)
  }

  function handleImageError() {
    if (key) imageCache.delete(key)
    setLoadedPreview({ key, preview: { name, imageUrl: null } })
  }

  return (
    <span className="card-name-preview" onPointerEnter={loadPreview} onFocus={loadPreview}>
      <button type="button" className="card-name-trigger" aria-label={`Ver imagem de ${name}`} onClick={loadPreview}>
        {prefix}{name}
      </button>
      <span className="card-name-popover" role="tooltip">
        {preview?.imageUrl ? (
          <img src={preview.imageUrl} alt={preview.name} loading="lazy" referrerPolicy="no-referrer" onError={handleImageError} />
        ) : (
          <span className="card-name-popover-empty">{loading ? 'Carregando imagem...' : 'Imagem indisponivel'}</span>
        )}
      </span>
    </span>
  )
}

function normalizeName(name) {
  return String(name || '').trim().toLowerCase()
}
