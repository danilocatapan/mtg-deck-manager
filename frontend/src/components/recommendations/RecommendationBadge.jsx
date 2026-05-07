const LABELS = {
  meta: 'Meta',
  synergy: 'Sinergia',
  curve: 'Curva',
  gap: 'Lacuna',
  draw: 'Draw',
  ramp: 'Ramp',
  removal: 'Remocao',
  protection: 'Protecao',
  finisher: 'Finalizador',
  efficiency: 'Eficiencia',
  fallback: 'Heuristico',
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
