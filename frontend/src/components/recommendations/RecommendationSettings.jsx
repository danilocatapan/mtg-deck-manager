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
    detail: 'Deck acima de um precon: curva mais consistente, mais ramp/compra/remoção e até algumas staples fortes, mas ainda sem plano de vencer cedo ou lockar a mesa.',
  },
  {
    value: 'high-power',
    label: 'Bracket 4 - Optimized',
    shortLabel: 'High-power',
    detail: 'Deck otimizado para ganhar com eficiência. Pode usar cartas muito fortes, combos e linhas duras, desde que a mesa saiba que a intenção é jogar high power.',
  },
  {
    value: 'cedh',
    label: 'Bracket 5 - cEDH',
    shortLabel: 'cEDH',
    detail: 'Deck competitivo, pensado para meta, velocidade, interação de stack, mulligans agressivos e linhas compactas de vitória. A prioridade é performance.',
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
        Nível do deck
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
          Apenas cartas que possuo
        </label>
        <label>
          <input type="checkbox" checked={filters.avoidSalt} onChange={() => toggleFilter('avoidSalt')} />
          Evitar cartas frustrantes
        </label>
        <label>
          <input type="checkbox" checked={filters.avoidTutors} onChange={() => toggleFilter('avoidTutors')} />
          Evitar tutores
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
          Mais interação
        </label>
        <label>
          <input type="checkbox" checked={filters.preserveTheme} onChange={() => toggleFilter('preserveTheme')} />
          Preservar tema
        </label>
      </fieldset>

      <Button type="submit" loading={loading} loadingLabel="Gerando trocas..." disabled={disabled}>
        Gerar trocas
      </Button>

      <details className="bracket-guide">
        <summary>Como escolher o bracket correto?</summary>
        <div className="bracket-guide-content">
          <p>
            Use o bracket como uma conversa de mesa, não como nota absoluta. O ponto principal é a intenção do deck:
            tema e experiência social nos brackets 1-3, otimização forte no 4, competição e meta no 5.
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
            Regra prática: se o deck ainda joga como precon melhorado, use Casual. Se já tem upgrades claros mas evita vencer cedo, use Bracket 3.
            Se aceita combos, staples de alto impacto e jogo mais duro, use Bracket 4. Se foi montado para torneio ou mesas cEDH, use Bracket 5.
          </p>
          <a href="https://magic.wizards.com/en/content/commander-format#brackets" target="_blank" rel="noreferrer">
            Ver explicação oficial de Commander Brackets na Wizards
          </a>
        </div>
      </details>
    </form>
  )
}
