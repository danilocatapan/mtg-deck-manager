import React, { useState } from 'react'
import Button from './ui/Button'

const BRACKETS = [
  { value: 'casual', label: 'Casual', detail: 'Sinergia temática, curva jogável e upgrades sem empurrar o deck para cEDH.' },
  { value: 'mid', label: 'Mid', detail: 'Equilíbrio entre plano do comandante, eficiência e staples razoáveis.' },
  { value: 'high-power', label: 'High-power', detail: 'Eficiência, curva baixa, interação, proteção e cortes mais duros.' },
  { value: 'cedh', label: 'cEDH', detail: 'Performance competitiva, velocidade, interação de stack e win conditions compactas.' },
]

const SOURCE_MODES = [
  { value: 'auto', label: 'Auto' },
  { value: 'casual_meta', label: 'Casual meta' },
  { value: 'competitive_meta', label: 'Competitive meta' },
  { value: 'cedh_only', label: 'cEDH only' },
  { value: 'local_only', label: 'Local only' },
]

export default function RecommendationForm({ onSubmit, disabled = false, onParamsChange }) {
  const [budget, setBudget] = useState(5)
  const [bracket, setBracket] = useState('casual')
  const [strategy, setStrategy] = useState('')
  const [sourceMode, setSourceMode] = useState('auto')
  const [maxRecommendations, setMaxRecommendations] = useState(5)
  const [showAdvanced, setShowAdvanced] = useState(false)

  const selectedBracket = BRACKETS.find((item) => item.value === bracket) || BRACKETS[0]

  function update(next) {
    const params = { budget, bracket, strategy, sourceMode, maxRecommendations, ...next }
    onParamsChange && onParamsChange(params)
    return params
  }

  function handleSubmit(e) {
    e.preventDefault()
    onSubmit(update())
  }

  return (
    <form onSubmit={handleSubmit} className="recommendation-form">
      <div className="recommendation-intel">
        <strong>Como a recomendação será calculada</strong>
        <p>O sistema escolhe fontes automaticamente pelo nível do deck, cruza isso com identidade de cor, legalidade Commander, sinergia do comandante, lacunas de função e qualidade do corte. A recomendação final sempre sai como troca add/remove explicada.</p>
      </div>

      <label title="Power bracket used to choose sources, baselines and cut strictness.">
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

      <label title="Approximate budget available for suggested cards.">
        Orçamento
        <small>Valor aproximado considerado para novas cartas.</small>
        <input type="number" min={0} value={budget} onChange={(e) => setBudget(parseInt(e.target.value || '0', 10))} />
      </label>

      <label title="Number of add/remove swaps to return.">
        Trocas
        <small>Quantidade de recomendações estratégicas.</small>
        <select
          value={maxRecommendations}
          onChange={(e) => {
            const nextValue = Number(e.target.value)
            setMaxRecommendations(nextValue)
            update({ maxRecommendations: nextValue })
          }}
        >
          <option value={3}>3</option>
          <option value={4}>4</option>
          <option value={5}>5</option>
        </select>
      </label>

      <label title="Optional plan for recommendation flavor, such as aggro, counters, tokens or graveyard.">
        Estratégia desejada
        <small>Preferência opcional para orientar sinergia.</small>
        <input value={strategy} onChange={(e) => setStrategy(e.target.value)} placeholder="Voltron, tokens, tribal, combo, controle..." />
      </label>

      <div className="advanced-source-panel">
        <button type="button" className="advanced-toggle" onClick={() => setShowAdvanced(!showAdvanced)}>
          {showAdvanced ? 'Ocultar fonte avançada' : 'Mostrar fonte avançada'}
        </button>
        {showAdvanced && (
          <label title="Advanced override for source selection. Auto is recommended for normal use.">
            Fonte de dados
            <small>Auto usa EDHREC/local para casual e TopDeck.gg, Spicerack e EDHTop16 para listas competitivas.</small>
            <select
              value={sourceMode}
              onChange={(e) => {
                setSourceMode(e.target.value)
                update({ sourceMode: e.target.value })
              }}
            >
              {SOURCE_MODES.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
            </select>
          </label>
        )}
      </div>

      <Button type="submit" disabled={disabled}>Gerar recomendações estratégicas</Button>
    </form>
  )
}
