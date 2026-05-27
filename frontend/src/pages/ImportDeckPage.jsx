import { useMemo, useState } from 'react'
import { importDeck } from '../services/api'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import PublicDeckPreview from '../components/PublicDeckPreview'
import importIcon from '../assets/icons/import.png'

const SAMPLE_DECK = `1 Sol Ring
1 Arcane Signet
12 Mountain
12 Forest`
const PREVIEW_COLLAPSED_LIMIT = 8
const IMPORT_FORMATS = [
  { value: 'MOXFIELD', label: 'Moxfield' },
  { value: 'MTG_ARENA', label: 'MTG Arena' },
  { value: 'MTGO', label: 'MTGO' },
  { value: 'ARCHIDEKT', label: 'Archidekt' },
  { value: 'PLAIN_TEXT', label: 'Plain Text' },
  { value: 'LIGAMAGIC', label: 'Liga Magic' },
]

function allowsPrintingMetadata(sourceFormat) {
  return ['MOXFIELD', 'MTG_ARENA', 'ARCHIDEKT'].includes(sourceFormat)
}

function parseCardText(value, sourceFormat) {
  let text = String(value || '').trim()
  let finish = ''
  if (!allowsPrintingMetadata(sourceFormat)) {
    return { name: text, setCode: '', collectorNumber: '', finish }
  }
  const finishMatch = text.match(/\s+\*(F|E)\*\s*$/i)
  if (finishMatch) {
    finish = finishMatch[1].toUpperCase() === 'F' ? 'foil' : 'etched'
    text = text.replace(/\s+\*(F|E)\*\s*$/i, '').trim()
  }

  let setCode = ''
  let collectorNumber = ''
  const numbered = text.match(/^(.*?)\s+\(([A-Za-z0-9]{2,8})\)\s+([A-Za-z0-9-]+)\s*$/)
  if (numbered) {
    text = numbered[1].trim()
    setCode = numbered[2].toUpperCase()
    collectorNumber = numbered[3]
  } else {
    const setOnly = text.match(/^(.*?)\s+\(([A-Za-z0-9]{2,8})\)\s*$/)
    if (setOnly) {
      text = setOnly[1].trim()
      setCode = setOnly[2].toUpperCase()
    }
  }

  return { name: text, setCode, collectorNumber, finish }
}

function printingLabel(card) {
  const edition = card.setCode ? `${card.setCode}${card.collectorNumber ? ` #${card.collectorNumber}` : ''}` : ''
  return [edition, card.finish].filter(Boolean).join(' - ')
}

function parsePreview(content, sourceFormat, commander = '') {
  if (!content.trim()) return { cards: [], errors: [], total: 0, rawTotal: 0, commanderInList: false, duplicates: [] }

  const cards = []
  const errors = []
  const normalizedCommander = normalizeName(commander)

  content.split(/\r?\n/).forEach((rawLine, index) => {
    const line = rawLine.trim()
    if (!line) return

    if (/^(deck|main deck|mainboard|commander|commanders|sideboard|maybeboard|tokens)$/i.test(line)
      || line.startsWith('//')
      || line.startsWith('#')
      || /^SB:/i.test(line)) {
      return
    }

    const match = line.match(/^(\d+)\s*x?\s+(.+)$/i)
    if (!match) {
      errors.push({ line: index + 1, message: `Linha ${index + 1}: use "quantidade nome da carta".` })
      return
    }

    const quantity = Number(match[1])
    const parsed = parseCardText(match[2], sourceFormat)
    const name = parsed.name
    if (quantity < 1 || !name) {
      errors.push({ line: index + 1, message: `Linha ${index + 1}: a quantidade deve ser maior que zero e o nome da carta e obrigatorio.` })
      return
    }

    cards.push({ quantity, ...parsed })
  })

  const mainDeckCards = normalizedCommander
    ? cards.filter((card) => normalizeName(card.name) !== normalizedCommander)
    : cards
  const commanderInList = normalizedCommander && cards.some((card) => normalizeName(card.name) === normalizedCommander)
  const duplicates = mainDeckCards.filter((card, index) => mainDeckCards.findIndex((item) => normalizeName(item.name) === normalizeName(card.name)) !== index)

  return {
    cards: mainDeckCards,
    allCards: cards,
    errors,
    total: mainDeckCards.reduce((sum, card) => sum + card.quantity, 0),
    rawTotal: cards.reduce((sum, card) => sum + card.quantity, 0),
    commanderInList,
    duplicates,
  }
}

function normalizeName(value) {
  return String(value || '').trim().toLowerCase()
}

export default function ImportDeckPage({ onDone }) {
  const [name, setName] = useState('')
  const [commander, setCommander] = useState('')
  const [visibility, setVisibility] = useState('private')
  const [sourceFormat, setSourceFormat] = useState('MOXFIELD')
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)
  const [previewExpanded, setPreviewExpanded] = useState(false)

  const preview = useMemo(() => parsePreview(content, sourceFormat, commander), [content, sourceFormat, commander])
  const isOverLimit = preview.total > 99
  const visiblePreviewCards = previewExpanded ? preview.cards : preview.cards.slice(0, PREVIEW_COLLAPSED_LIMIT)
  const hiddenPreviewCount = Math.max(0, preview.cards.length - visiblePreviewCards.length)
  const localValidationItems = [
    { label: 'Total', value: `${preview.total}/99${preview.commanderInList ? ' + comandante' : ''}`, tone: isOverLimit ? 'bad' : preview.total === 99 ? 'good' : 'warning' },
    { label: 'Linhas invalidas', value: preview.errors.length, tone: preview.errors.length ? 'bad' : 'good' },
    { label: 'Duplicadas', value: preview.duplicates.length, tone: preview.duplicates.length ? 'warning' : 'good' },
  ]
  const backendValidationItems = [
    { label: 'Cores', value: 'Apos salvar', tone: 'warning' },
    { label: 'Singleton', value: 'Apos resolver', tone: 'warning' },
    { label: 'Banlist', value: 'Backend', tone: 'warning' },
  ]
  const fieldErrors = useMemo(() => {
    const nextErrors = {}
    if (!name.trim()) nextErrors.name = 'Informe o nome do deck.'
    if (!commander.trim()) nextErrors.commander = 'Informe o comandante.'
    if (!content.trim()) nextErrors.content = 'Cole uma lista ou envie um arquivo .txt.'
    if (preview.errors.length > 0) nextErrors.content = 'Corrija as linhas invalidas antes de salvar.'
    if (isOverLimit) nextErrors.content = `O deck importado tem ${preview.total} cartas; o maximo e 99.`
    if (content.trim() && preview.cards.length === 0) nextErrors.content = 'Nenhuma carta valida foi lida da lista.'
    return nextErrors
  }, [commander, content, isOverLimit, name, preview.cards.length, preview.errors.length, preview.total])
  const visibleFieldErrors = fieldErrors
  const canImport = Object.keys(fieldErrors).length === 0
  const disabledReason = !canImport ? Object.values(fieldErrors)[0] : ''

  const handleFile = async (file) => {
    if (!file) return
    const text = await file.text()
    setContent(text)
  }

  const handleSubmit = async () => {
    try {
      setError(null)
      setMessage(null)
      if (!canImport) {
        setError('Revise a pre-validacao local antes de importar.')
        return
      }

      setLoading(true)
      const created = await importDeck({ name: name.trim(), commander: commander.trim(), content, visibility, sourceFormat })
      setMessage(`${created.name} importado.`)
      onDone && onDone(`${created.name} importado.`, created)
    } catch (e) {
      setError(e.message || 'Falha na importacao.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="import-deck-page">
      <section className="zone zone-command page-heading">
        <div>
          <p className="eyebrow">Command Zone</p>
          <h1>Importar Deck</h1>
          <p className="page-description">Cole uma lista Commander com uma carta por linha. O preview roda antes de salvar; cores sao validadas apos resolver as cartas no backend.</p>
        </div>
        <Button variant="secondary" onClick={() => onDone && onDone()}>Voltar aos Decks</Button>
      </section>

      {message && <div className="status success">{message}</div>}
      {error && <div className="status error">{error}</div>}

      <div className="split-layout">
        <Card className="zone zone-library">
          <div className="validation-summary" aria-label="Resumo da validacao de importacao">
            <div className="validation-group" aria-label="Pre-validacao local">
              <strong>Pre-validacao local</strong>
              {localValidationItems.map((item) => (
                <div key={item.label} className={`validation-item ${item.tone}`}>
                  <span>{item.label}</span>
                  <strong>{item.value}</strong>
                </div>
              ))}
            </div>
            <div className="validation-group" aria-label="Validacao apos salvar">
              <strong>Validacao apos salvar</strong>
              {backendValidationItems.map((item) => (
                <div key={item.label} className={`validation-item ${item.tone}`}>
                  <span>{item.label}</span>
                  <strong>{item.value}</strong>
                </div>
              ))}
            </div>
          </div>

          <div className="form-grid">
            <label>
              Nome do deck
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Gruul Revels"
                aria-invalid={Boolean(visibleFieldErrors.name)}
                aria-describedby={visibleFieldErrors.name ? 'import-name-error' : undefined}
              />
              {visibleFieldErrors.name && <span id="import-name-error" className="field-error">{visibleFieldErrors.name}</span>}
            </label>
            <label>
              Comandante
              <input
                value={commander}
                onChange={(e) => setCommander(e.target.value)}
                placeholder="Xenagos, God of Revels"
                aria-invalid={Boolean(visibleFieldErrors.commander)}
                aria-describedby={visibleFieldErrors.commander ? 'import-commander-error' : undefined}
              />
              {visibleFieldErrors.commander && <span id="import-commander-error" className="field-error">{visibleFieldErrors.commander}</span>}
            </label>
            <label>
              Origem da exportacao
              <small>Use a origem para preservar edicao, numero e foil quando a lista trouxer esses dados.</small>
              <select value={sourceFormat} onChange={(e) => setSourceFormat(e.target.value)}>
                {IMPORT_FORMATS.map((format) => (
                  <option key={format.value} value={format.value}>{format.label}</option>
                ))}
              </select>
            </label>
            <label>
              Visibilidade
              <small>Decks publicos aparecem na vitrine e podem ser copiados por outros usuarios.</small>
              <select value={visibility} onChange={(e) => setVisibility(e.target.value)}>
                <option value="private">Privado</option>
                <option value="public">Publico</option>
              </select>
              <small className="privacy-microcopy">Privado e o padrao. Publico entra na vitrine e pode ser copiado, sem expor e-mail, dono tecnico ou historico.</small>
            </label>
          </div>

          {visibility === 'public' && (
            <PublicDeckPreview name={name} commander={commander} total={preview.total} />
          )}

          <label>
            Colar lista do deck
            <small>Formato: quantidade seguida pelo nome exato da carta.</small>
            <textarea
              rows={14}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder={SAMPLE_DECK}
              aria-invalid={Boolean(visibleFieldErrors.content)}
              aria-describedby={visibleFieldErrors.content ? 'import-content-error' : undefined}
            />
            {visibleFieldErrors.content && <span id="import-content-error" className="field-error">{visibleFieldErrors.content}</span>}
          </label>

          <label>
            Enviar arquivo .txt
            <input type="file" accept=".txt" onChange={(e) => handleFile(e.target.files?.[0])} />
          </label>

          <div className="form-actions">
            {disabledReason && <span id="import-disabled-reason" className="empty-action-hint">{disabledReason}</span>}
            <Button onClick={handleSubmit} disabled={loading || !canImport} loading={loading} loadingLabel="Importando deck..." aria-describedby={disabledReason ? 'import-disabled-reason' : undefined}>
              <img className="btn-icon" src={importIcon} alt="" aria-hidden="true" />
              Importar Deck
            </Button>
            <Button variant="secondary" onClick={() => setContent(SAMPLE_DECK)}>Usar exemplo</Button>
          </div>
        </Card>

        <Card className="zone zone-planning">
          <div className="section-heading">
            <div>
              <h2>Preview</h2>
              <p className={isOverLimit ? 'is-invalid' : ''}>
                {preview.total}/99 cartas lidas{preview.commanderInList ? ` + comandante (${preview.rawTotal} linhas)` : ''}
              </p>
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
              {commander.trim() && (
                <div className="deck-row preview-commander-row">
                  <strong>{commander.trim()}</strong>
                  <span>Comandante</span>
                </div>
              )}
              {visiblePreviewCards.map((card, index) => (
                <div key={`${card.name}-${index}`} className="deck-row">
                  <strong>{card.name}</strong>
                  <span>{card.quantity}x{printingLabel(card) ? ` - ${printingLabel(card)}` : ''}</span>
                </div>
              ))}
              {preview.cards.length > PREVIEW_COLLAPSED_LIMIT && (
                <Button
                  variant="secondary"
                  className="preview-toggle-button"
                  onClick={() => setPreviewExpanded((expanded) => !expanded)}
                >
                  {previewExpanded ? 'Mostrar menos' : `Mostrar mais ${hiddenPreviewCount} linhas`}
                </Button>
              )}
            </div>
          )}
        </Card>
      </div>
    </section>
  )
}
