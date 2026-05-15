const LABELS = {
  meta: 'Meta',
  synergy: 'Sinergia',
  curve: 'Curva',
  gap: 'Lacuna',
  draw: 'Draw',
  ramp: 'Ramp',
  removal: 'Remoção',
  protection: 'Proteção',
  finisher: 'Finalizador',
  efficiency: 'Eficiência',
  fallback: 'Heurístico',
  high: 'Alta confiança',
  medium: 'Média confiança',
  low: 'Baixa confiança',
  value: 'Valor',
}

export default function RecommendationBadge({ variant = 'value', children }) {
  const safeVariant = LABELS[variant] ? variant : 'value'
  return (
    <span className={`recommendation-badge ${safeVariant}`}>
      {children || LABELS[safeVariant]}
    </span>
  )
}
