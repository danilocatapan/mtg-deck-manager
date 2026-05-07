import { useState } from 'react'
import Button from './ui/Button'

const BRACKETS = [
  { value: 'casual', label: 'Casual', detail: 'Sinergia tematica, curva jogavel e upgrades sem empurrar o deck para cEDH.' },
  { value: 'mid', label: 'Mid', detail: 'Equilibrio entre plano do comandante, eficiencia e staples razoaveis.' },
  { value: 'high-power', label: 'High-power', detail: 'Eficiencia, curva baixa, interacao, protecao e cortes mais duros.' },
  { value: 'cedh', label: 'cEDH', detail: 'Performance competitiva, velocidade, interacao de stack e win conditions compactas.' },
]

export default function RecommendationForm({ onSubmit, disabled = false, onParamsChange }) {
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
    <form onSubmit={handleSubmit} className="recommendation-form">
      <div className="recommendation-intel">
        <strong>Como a recomendacao sera calculada</strong>
        <p>Escolha apenas o bracket. O backend cruza deck atual, comandante, identidade de cor e perfil meta local para sugerir de 3 a 5 trocas add/remove com explicacao clara.</p>
      </div>

      <label title="Power bracket used to choose baselines, scoring weights and cut strictness.">
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

      <Button type="submit" disabled={disabled}>Gerar recomendacoes estrategicas</Button>
    </form>
  )
}
