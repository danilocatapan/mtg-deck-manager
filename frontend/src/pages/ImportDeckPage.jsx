import { useMemo, useState } from 'react'
import { importDeck } from '../services/api'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import importIcon from '../assets/icons/import.png'

const SAMPLE_DECK = `1 Sol Ring
1 Arcane Signet
12 Mountain
12 Forest`

function parsePreview(content) {
  if (!content.trim()) return { cards: [], errors: [], total: 0, duplicates: [] }

  const cards = []
  const errors = []

  content.split(/\r?\n/).forEach((rawLine, index) => {
    const line = rawLine.trim()
    if (!line) return

    const match = line.match(/^(\d+)\s+(.+)$/)
    if (!match) {
      errors.push({ line: index + 1, message: `Linha ${index + 1}: use "quantidade nome da carta".` })
      return
    }

    const quantity = Number(match[1])
    const name = match[2].trim()
    if (quantity < 1 || !name) {
      errors.push({ line: index + 1, message: `Linha ${index + 1}: a quantidade deve ser maior que zero e o nome da carta é obrigatório.` })
      return
    }

    cards.push({ quantity, name })
  })

  const duplicates = cards.filter((card, index) => cards.findIndex((item) => item.name.toLowerCase() === card.name.toLowerCase()) !== index)

  return {
    cards,
    errors,
    total: cards.reduce((sum, card) => sum + card.quantity, 0),
    duplicates,
  }
}

export default function ImportDeckPage({ onDone }) {
  const [name, setName] = useState('')
  const [commander, setCommander] = useState('')
  const [visibility, setVisibility] = useState('private')
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)

  const preview = useMemo(() => parsePreview(content), [content])
  const isOverLimit = preview.total > 99
  const validationItems = [
    { label: 'Total', value: `${preview.total}/99`, tone: isOverLimit ? 'bad' : preview.total === 99 ? 'good' : 'warning' },
    { label: 'Linhas invalidas', value: preview.errors.length, tone: preview.errors.length ? 'bad' : 'good' },
    { label: 'Duplicadas', value: preview.duplicates.length, tone: preview.duplicates.length ? 'warning' : 'good' },
    { label: 'Cores', value: 'Pendente', tone: 'warning' },
  ]
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
        setError('Nome do deck e comandante são obrigatórios.')
        return
      }
      if (preview.errors.length > 0) {
        setError('Corrija as linhas destacadas antes de salvar.')
        return
      }
      if (isOverLimit) {
        setError(`O deck importado tem ${preview.total} cartas; o máximo é 99.`)
        return
      }
      if (preview.cards.length === 0) {
        setError('Cole ou envie uma lista antes de importar.')
        return
      }

      setLoading(true)
      const created = await importDeck({ name: name.trim(), commander: commander.trim(), content, visibility })
      setMessage(`${created.name} importado.`)
      onDone && onDone(`${created.name} importado.`, created)
    } catch (e) {
      setError(e.message || 'Falha na importação.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main>
      <section className="zone zone-command page-heading">
        <div>
          <p className="eyebrow">Command Zone</p>
          <h1>Importar Deck</h1>
          <p className="page-description">Cole uma lista Commander com uma carta por linha. O preview roda antes de salvar; cores são validadas após resolver as cartas no backend.</p>
        </div>
        <Button variant="secondary" onClick={() => onDone && onDone()}>Voltar aos Decks</Button>
      </section>

      {message && <div className="status success">{message}</div>}
      {error && <div className="status error">{error}</div>}

      <div className="split-layout">
        <Card className="zone zone-library">
          <div className="validation-summary" aria-label="Import validation summary">
            {validationItems.map((item) => (
              <div key={item.label} className={`validation-item ${item.tone}`}>
                <span>{item.label}</span>
                <strong>{item.value}</strong>
              </div>
            ))}
          </div>

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

          <label>
            Colar lista do deck
            <small>Formato: quantidade seguida pelo nome exato da carta.</small>
            <textarea rows={14} value={content} onChange={(e) => setContent(e.target.value)} placeholder={SAMPLE_DECK} />
          </label>

          <label>
            Enviar arquivo .txt
            <input type="file" accept=".txt" onChange={(e) => handleFile(e.target.files?.[0])} />
          </label>

          <div className="form-actions">
            <Button onClick={handleSubmit} disabled={loading || !canImport}>
              <img className="btn-icon" src={importIcon} alt="" aria-hidden="true" />
              {loading ? 'Importando...' : 'Importar Deck'}
            </Button>
            <Button variant="secondary" onClick={() => setContent(SAMPLE_DECK)}>Usar exemplo</Button>
          </div>
        </Card>

        <Card className="zone zone-planning">
          <div className="section-heading">
            <div>
              <h2>Preview</h2>
              <p className={isOverLimit ? 'is-invalid' : ''}>{preview.total}/99 cartas lidas</p>
            </div>
          </div>

          {preview.errors.length > 0 && (
            <div className="status error">
              {preview.errors.map((item) => <div key={item.line}>{item.message}</div>)}
            </div>
          )}

          {preview.cards.length === 0 ? (
            <div className="empty-inline">Cole uma lista para visualizar quantidades antes de salvar.</div>
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
