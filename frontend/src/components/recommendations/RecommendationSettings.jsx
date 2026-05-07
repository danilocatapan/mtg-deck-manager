import { useState } from 'react'
import Button from '../ui/Button'

const BRACKETS = [
  { value: 'casual', label: 'Casual', detail: 'Sinergia tematica, curva jogavel e upgrades sem empurrar o deck para cEDH.' },
  { value: 'mid', label: 'Mid', detail: 'Equilibrio entre plano do comandante, eficiencia e staples razoaveis.' },
  { value: 'high-power', label: 'High-power', detail: 'Eficiencia, curva baixa, interacao, protecao e cortes mais duros.' },
  { value: 'cedh', label: 'cEDH', detail: 'Performance competitiva, velocidade, interacao de stack e win conditions compactas.' },
]

export default function RecommendationSettings({ onSubmit, disabled = false, loading = false, onParamsChange }) {
  const [bracket, setBracket] = useState('casual')
  const selectedBracket = BRACKETS.find((item) => item.value === bracket) || BRACKETS[0]

  function update(next) {
    const params = { bracket, ...next }
    onParamsChange && onParamsChange(params)
    return params
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

      <Button type="submit" loading={loading} disabled={disabled}>
        Gerar trocas
      </Button>
    </form>
  )
}
