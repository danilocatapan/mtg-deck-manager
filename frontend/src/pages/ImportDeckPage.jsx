import React, { useMemo, useState } from 'react'
import { importDeck } from '../services/api'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import importIcon from '../assets/icons/import.png'

const SAMPLE_DECK = `1 Sol Ring
1 Arcane Signet
12 Mountain
12 Forest`

function parsePreview(content) {
  if (!content.trim()) return { cards: [], errors: [], total: 0 }

  const cards = []
  const errors = []

  content.split(/\r?\n/).forEach((rawLine, index) => {
    const line = rawLine.trim()
    if (!line) return

    const match = line.match(/^(\d+)\s+(.+)$/)
    if (!match) {
      errors.push({ line: index + 1, message: `Line ${index + 1}: use "quantity card name".` })
      return
    }

    const quantity = Number(match[1])
    const name = match[2].trim()
    if (quantity < 1 || !name) {
      errors.push({ line: index + 1, message: `Line ${index + 1}: quantity must be greater than zero and card name is required.` })
      return
    }

    cards.push({ quantity, name })
  })

  return {
    cards,
    errors,
    total: cards.reduce((sum, card) => sum + card.quantity, 0),
  }
}

export default function ImportDeckPage({ onDone }) {
  const [name, setName] = useState('')
  const [commander, setCommander] = useState('')
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)

  const preview = useMemo(() => parsePreview(content), [content])
  const isOverLimit = preview.total > 99
  const canImport = name.trim() && commander.trim() && preview.cards.length > 0 && preview.errors.length === 0 && !isOverLimit

  const handleFile = async (file) => {
    if (!file) return
    const text = await file.text()
    setContent(text)
  }

  const handleSubmit = async () => {
    try {
      setError(null)
      setMessage(null)
      if (!name.trim() || !commander.trim()) {
        setError('Deck name and commander are required.')
        return
      }
      if (preview.errors.length > 0) {
        setError('Fix the highlighted import lines before saving.')
        return
      }
      if (isOverLimit) {
        setError(`Imported deck has ${preview.total} cards; maximum is 99.`)
        return
      }
      if (preview.cards.length === 0) {
        setError('Paste or upload a deck list before importing.')
        return
      }

      setLoading(true)
      const created = await importDeck({ name: name.trim(), commander: commander.trim(), content })
      setMessage(`Imported ${created.name}.`)
      onDone && onDone(`Imported ${created.name}.`)
    } catch (e) {
      setError(e.message || 'Import failed.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main>
      <section className="page-heading">
        <div>
          <p className="eyebrow">Import deck</p>
          <h1>Import Deck</h1>
          <p className="page-description">Paste a Commander list using one card per line. Preview and validation run before anything is saved.</p>
        </div>
        <Button variant="secondary" onClick={() => onDone && onDone()}>Back to Decks</Button>
      </section>

      {message && <div className="status success">{message}</div>}
      {error && <div className="status error">{error}</div>}

      <div className="split-layout">
        <Card>
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

          <label>
            Paste deck list
            <small>Format: quantity followed by exact card name.</small>
            <textarea rows={14} value={content} onChange={(e) => setContent(e.target.value)} placeholder={SAMPLE_DECK} />
          </label>

          <label>
            Upload .txt file
            <input type="file" accept=".txt" onChange={(e) => handleFile(e.target.files?.[0])} />
          </label>

          <div className="form-actions">
            <Button onClick={handleSubmit} disabled={loading || !canImport}>
              <img className="btn-icon" src={importIcon} alt="" aria-hidden="true" />
              {loading ? 'Importing...' : 'Import Deck'}
            </Button>
            <Button variant="secondary" onClick={() => setContent(SAMPLE_DECK)}>Use Example</Button>
          </div>
        </Card>

        <Card>
          <div className="section-heading">
            <div>
              <h2>Preview</h2>
              <p className={isOverLimit ? 'is-invalid' : ''}>{preview.total}/99 cards parsed</p>
            </div>
          </div>

          {preview.errors.length > 0 && (
            <div className="status error">
              {preview.errors.map((item) => <div key={item.line}>{item.message}</div>)}
            </div>
          )}

          {preview.cards.length === 0 ? (
            <div className="empty-inline">Paste a list to preview card quantities before saving.</div>
          ) : (
            <div className="deck-table compact">
              {preview.cards.map((card, index) => (
                <div key={`${card.name}-${index}`} className="deck-row">
                  <strong>{card.name}</strong>
                  <span>{card.quantity}x</span>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </main>
  )
}
