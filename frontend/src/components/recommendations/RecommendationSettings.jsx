import { useState } from 'react'
import Button from '../ui/Button'

const BRACKETS = [
  {
    value: 'casual',
    label: 'Bracket 2 - Core / Casual',
    shortLabel: 'Casual',
    detail: 'Deck focado em tema e jogo social, parecido com precon ajustado. Evite combos infinitos intencionais de duas cartas, mass land denial e excesso de Game Changers.',
  },
  {
    value: 'mid',
    label: 'Bracket 3 - Upgraded',
    shortLabel: 'Mid',
    detail: 'Deck acima de um precon: curva mais consistente, mais ramp/compra/remocao e ate algumas staples fortes, mas ainda sem plano de vencer cedo ou lockar a mesa.',
  },
  {
    value: 'high-power',
    label: 'Bracket 4 - Optimized',
    shortLabel: 'High-power',
    detail: 'Deck otimizado para ganhar com eficiencia. Pode usar cartas muito fortes, combos e linhas duras, desde que a mesa saiba que a intencao e jogar high power.',
  },
  {
    value: 'cedh',
    label: 'Bracket 5 - cEDH',
    shortLabel: 'cEDH',
    detail: 'Deck competitivo, pensado para meta, velocidade, interacao de stack, mulligans agressivos e linhas compactas de vitoria. A prioridade e performance.',
  },
]

export default function RecommendationSettings({ onSubmit, disabled = false, loading = false, onParamsChange }) {
  const [bracket, setBracket] = useState('casual')
  const [filters, setFilters] = useState({
    ownedOnly: false,
    avoidSalt: false,
    avoidTutors: false,
    improveMana: false,
    lowerCurve: false,
    moreInteraction: false,
    preserveTheme: false,
  })
  const selectedBracket = BRACKETS.find((item) => item.value === bracket) || BRACKETS[0]

  function update(next) {
    const params = { bracket, ...filters, ...next }
    onParamsChange && onParamsChange(params)
    return params
  }

  function toggleFilter(key) {
    const nextFilters = { ...filters, [key]: !filters[key] }
    setFilters(nextFilters)
    update(nextFilters)
  }

  function handleSubmit(e) {
    e.preventDefault()
    onSubmit(update())
  }

  return (
    <form onSubmit={handleSubmit} className="recommendation-settings">
      <label title="Power bracket usado para baselines, pesos de score e rigor dos cortes.">
        Nivel do deck
        <small>{selectedBracket.detail}</small>
        <select
          value={bracket}
          onChange={(e) => {
            setBracket(e.target.value)
            update({ bracket: e.target.value })
          }}
        >
          {BRACKETS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
        </select>
      </label>

      <fieldset className="recommendation-filters">
        <legend>Filtros</legend>
        <label>
          <input type="checkbox" checked={filters.ownedOnly} onChange={() => toggleFilter('ownedOnly')} />
          Owned-only
        </label>
        <label>
          <input type="checkbox" checked={filters.avoidSalt} onChange={() => toggleFilter('avoidSalt')} />
          Evitar salt
        </label>
        <label>
          <input type="checkbox" checked={filters.avoidTutors} onChange={() => toggleFilter('avoidTutors')} />
          Evitar tutors
        </label>
        <label>
          <input type="checkbox" checked={filters.improveMana} onChange={() => toggleFilter('improveMana')} />
          Melhorar mana
        </label>
        <label>
          <input type="checkbox" checked={filters.lowerCurve} onChange={() => toggleFilter('lowerCurve')} />
          Baixar curva
        </label>
        <label>
          <input type="checkbox" checked={filters.moreInteraction} onChange={() => toggleFilter('moreInteraction')} />
          Mais interacao
        </label>
        <label>
          <input type="checkbox" checked={filters.preserveTheme} onChange={() => toggleFilter('preserveTheme')} />
          Preservar tema
        </label>
      </fieldset>

      <Button type="submit" loading={loading} disabled={disabled}>
        Gerar trocas
      </Button>

      <details className="bracket-guide">
        <summary>Como escolher o bracket correto?</summary>
        <div className="bracket-guide-content">
          <p>
            Use o bracket como uma conversa de mesa, nao como nota absoluta. O ponto principal e a intencao do deck:
            tema e experiencia social nos brackets 1-3, otimizacao forte no 4, competicao e meta no 5.
          </p>
          <div className="bracket-guide-grid">
            {BRACKETS.map((item) => (
              <article key={item.value}>
                <strong>{item.label}</strong>
                <span>{item.detail}</span>
              </article>
            ))}
          </div>
          <p>
            Regra pratica: se o deck ainda joga como precon melhorado, use Casual. Se ja tem upgrades claros mas evita vencer cedo, use Bracket 3.
            Se aceita combos, staples de alto impacto e jogo mais duro, use Bracket 4. Se foi montado para torneio ou mesas cEDH, use Bracket 5.
          </p>
          <a href="https://magic.wizards.com/en/content/commander-format#brackets" target="_blank" rel="noreferrer">
            Ver explicacao oficial de Commander Brackets na Wizards
          </a>
        </div>
      </details>
    </form>
  )
}
