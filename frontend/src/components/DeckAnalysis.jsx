export default function DeckAnalysis({ analysis }) {
  if (!analysis) return null

  const formatMetric = (value) => {
    if (typeof value !== 'number') return value ?? '-'
    if (!Number.isFinite(value)) return '-'
    return Number.isInteger(value) ? value : value.toFixed(2)
  }
  const percent = (value) => `${Math.round(Number(value || 0) * 100)}%`

  const metrics = [
    ['Average CMC', analysis.averageCmc],
    ['Total Cards', analysis.totalCards],
    ['Ramp', analysis.rampCount],
    ['Draw', analysis.drawCount],
    ['Removal', analysis.removalCount],
  ]
  const diagnostics = [
    { label: 'Early Game', value: (analysis.manaCurve?.[0] || 0) + (analysis.manaCurve?.[1] || 0) + (analysis.manaCurve?.[2] || 0), goodAt: 16 },
    { label: 'Ramp Density', value: analysis.rampCount ?? 0, goodAt: 10 },
    { label: 'Interaction', value: analysis.removalCount ?? 0, goodAt: 8 },
    { label: 'Card Advantage', value: analysis.drawCount ?? 0, goodAt: 8 },
  ].map((item) => ({
    ...item,
    tone: item.value >= item.goodAt ? 'good' : item.value >= Math.ceil(item.goodAt * 0.65) ? 'warning' : 'bad',
  }))

  return (
    <div className="analysis-panel">
      <div className="metric-grid">
        {metrics.map(([label, value]) => (
          <div key={label} className="metric-card">
            <span>{label}</span>
            <strong title={String(value)}>{formatMetric(value)}</strong>
          </div>
        ))}
      </div>

      {analysis.score && (
        <>
          <h4>Score explicavel</h4>
          <div className="diagnostic-grid">
            {[
              ['Velocidade', analysis.score.speed],
              ['Consistencia', analysis.score.consistency],
              ['Interacao', analysis.score.interaction],
              ['Resiliencia', analysis.score.resilience],
              ['Ameacas', analysis.score.threat],
              ['Pressao bracket', analysis.score.bracketPressure],
            ].map(([label, value]) => (
              <div key={label} className={`diagnostic-card metric-${scoreTone(value)}`}>
                <span>{label}</span>
                <strong>{formatMetric(value)}</strong>
                <small>/100</small>
              </div>
            ))}
          </div>
          {analysis.score.summary && <p className="analysis-note">{analysis.score.summary}</p>}
        </>
      )}

      <h4>Commander Diagnostics</h4>
      <div className="diagnostic-grid">
        {diagnostics.map((item) => (
          <div key={item.label} className={`diagnostic-card metric-${item.tone}`}>
            <span>{item.label}</span>
            <strong>{formatMetric(item.value)}</strong>
            <small>Target {item.goodAt}+</small>
          </div>
        ))}
      </div>

      {analysis.roles && (
        <>
          <h4>Funcoes do deck</h4>
          <div className="gap-grid">
            {Object.entries(analysis.roles).map(([role, count]) => (
              <div key={role} className="gap-item">
                <span>{roleLabel(role)}</span>
                <strong>{count}</strong>
              </div>
            ))}
          </div>
        </>
      )}

      {analysis.manaBase && (
        <>
          <h4>Mana base</h4>
          <div className="mana-diagnostic-grid">
            <div className="diagnostic-card">
              <span>Terrenos</span>
              <strong>{analysis.manaBase.landCount ?? 0}</strong>
              <small>{analysis.manaBase.tappedLandCount ?? 0} entram virados</small>
            </div>
            <div className="diagnostic-card">
              <span>Fontes untapped T1/T2/T3</span>
              <strong>
                {analysis.manaBase.untappedSourcesByTurn?.[1] ?? 0}/
                {analysis.manaBase.untappedSourcesByTurn?.[2] ?? 0}/
                {analysis.manaBase.untappedSourcesByTurn?.[3] ?? 0}
              </strong>
              <small>terrenos e fontes baratas</small>
            </div>
          </div>
          <div className="color-grid">
            {['W', 'U', 'B', 'R', 'G', 'C'].map((color) => (
              <div key={color} className="color-item">
                <span>{color}</span>
                <strong>{analysis.manaBase.colorSources?.[color] ?? 0}</strong>
                <small>custo {analysis.manaBase.colorCosts?.[color] ?? 0}</small>
              </div>
            ))}
          </div>
        </>
      )}

      {analysis.probabilities && (
        <>
          <h4>Probabilidades</h4>
          <div className="diagnostic-grid">
            <div className="diagnostic-card">
              <span>Mao inicial com 2+ terrenos</span>
              <strong>{percent(analysis.probabilities.openingHandTwoPlusLands)}</strong>
            </div>
            <div className="diagnostic-card">
              <span>Ramp ate turno 2</span>
              <strong>{percent(analysis.probabilities.rampByTurnTwo)}</strong>
            </div>
            <div className="diagnostic-card">
              <span>Interacao ate turno 3</span>
              <strong>{percent(analysis.probabilities.interactionByTurnThree)}</strong>
            </div>
          </div>
        </>
      )}

      {analysis.combos && (
        <>
          <h4>Combos</h4>
          <div className="combo-grid">
            <div className="combo-section">
              <span className="rec-label">Presentes</span>
              {analysis.combos.present?.length ? analysis.combos.present.map((combo) => (
                <article key={combo.name}>
                  <strong>{combo.name}</strong>
                  <span>{combo.result}</span>
                  <small>{combo.source}</small>
                </article>
              )) : <div className="empty-inline">Nenhum combo conhecido detectado.</div>}
            </div>
            <div className="combo-section">
              <span className="rec-label">A 1 carta de distancia</span>
              {analysis.combos.oneCardAway?.length ? analysis.combos.oneCardAway.map((combo) => (
                <article key={combo.name}>
                  <strong>{combo.name}</strong>
                  <span>Falta: {combo.missingCard}</span>
                  <small>{combo.result}</small>
                </article>
              )) : <div className="empty-inline">Nenhuma linha a uma carta detectada.</div>}
            </div>
          </div>
        </>
      )}

      <h4>Mana Curve</h4>
      <div className="mana-curve">
        {analysis.manaCurve && Object.entries(analysis.manaCurve).map(([cmc, count]) => (
          <div key={cmc} className="curve-row">
            <span>{cmc}</span>
            <div>
              <i style={{ width: `${Math.max(8, Number(count) * 16)}px` }} />
            </div>
            <strong>{count}</strong>
          </div>
        ))}
      </div>

      {analysis.manaCurveByType && (
        <>
          <h4>Curva por tipo</h4>
          <div className="curve-type-grid">
            {Object.entries(analysis.manaCurveByType).map(([type, curve]) => (
              <div key={type} className="curve-type-card">
                <strong>{roleLabel(type)}</strong>
                <span>{Object.entries(curve).map(([cmc, count]) => `${cmc}: ${count}`).join(' / ')}</span>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

function roleLabel(role) {
  return {
    ramp: 'Ramp',
    draw: 'Compra',
    interaction: 'Interacao',
    protection: 'Protecao',
    boardWipe: 'Wipes',
    wincon: 'Wincons',
    land: 'Terrenos',
    creature: 'Criaturas',
    artifact: 'Artefatos',
    enchantment: 'Encantamentos',
    instant: 'Instantaneas',
    sorcery: 'Feiticos',
    planeswalker: 'Planeswalkers',
    other: 'Outros',
  }[role] || role
}

function scoreTone(value) {
  if (Number(value) >= 70) return 'good'
  if (Number(value) >= 45) return 'warning'
  return 'bad'
}
