export default function DeckAnalysis({ analysis }) {
  if (!analysis) return null

  const formatMetric = (value) => {
    if (typeof value !== 'number') return value ?? '-'
    if (!Number.isFinite(value)) return '-'
    return Number.isInteger(value) ? value : value.toFixed(2)
  }
  const percent = (value) => `${Math.round(Number(value || 0) * 100)}%`

  const metrics = [
    ['CMC medio', analysis.averageCmc],
    ['Cartas no deck', analysis.totalCards],
    ['Ramp', analysis.rampCount],
    ['Compra', analysis.drawCount],
    ['Interacao', analysis.removalCount],
  ]
  const diagnostics = [
    { label: 'Jogo inicial', value: (analysis.manaCurve?.[0] || 0) + (analysis.manaCurve?.[1] || 0) + (analysis.manaCurve?.[2] || 0), goodAt: 16, action: 'Adicione cartas de custo 0-2 para reduzir maos lentas.' },
    { label: 'Densidade de ramp', value: analysis.rampCount ?? 0, goodAt: 10, action: 'Priorize ramp de custo baixo ou fixing se estiver abaixo do alvo.' },
    { label: 'Interacao', value: analysis.removalCount ?? 0, goodAt: 8, action: 'Inclua respostas pontuais ou interacao de pilha conforme o bracket.' },
    { label: 'Card advantage', value: analysis.drawCount ?? 0, goodAt: 8, action: 'Aumente compra, selecao ou motores de valor recorrente.' },
  ].map((item) => ({
    ...item,
    tone: item.value >= item.goodAt ? 'good' : item.value >= Math.ceil(item.goodAt * 0.65) ? 'warning' : 'bad',
  }))
  const actionableDiagnostics = diagnostics
    .filter((item) => item.tone !== 'good')
    .slice(0, 3)
  const scoreAlerts = [
    analysis.score?.speed < 45 ? 'A velocidade esta baixa: revise curva e ramp inicial.' : null,
    analysis.score?.interaction < 45 ? 'A interacao esta baixa: o deck pode ter dificuldade para responder ameacas.' : null,
    analysis.score?.consistency < 45 ? 'A consistencia esta baixa: aumente compra, selecao ou redundancia funcional.' : null,
    analysis.score?.threat < 45 ? 'As ameacas estao baixas: confirme como o deck fecha a partida.' : null,
  ].filter(Boolean)

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

      {(actionableDiagnostics.length > 0 || scoreAlerts.length > 0) && (
        <>
          <h4>Diagnostico acionavel</h4>
          <div className="diagnostic-grid">
            {actionableDiagnostics.map((item) => (
              <div key={item.label} className={`diagnostic-card metric-${item.tone}`}>
                <span>{item.label}</span>
                <strong>{diagnosticTitle(item.tone)}</strong>
                <small>{item.action}</small>
              </div>
            ))}
            {scoreAlerts.slice(0, Math.max(0, 3 - actionableDiagnostics.length)).map((alert) => (
              <div key={alert} className="diagnostic-card metric-warning">
                <span>Score explicavel</span>
                <strong>Ajustar</strong>
                <small>{alert}</small>
              </div>
            ))}
          </div>
        </>
      )}

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

      <h4>Diagnosticos Commander</h4>
      <div className="diagnostic-grid">
        {diagnostics.map((item) => (
          <div key={item.label} className={`diagnostic-card metric-${item.tone}`}>
            <span>{item.label}</span>
            <strong>{formatMetric(item.value)}</strong>
            <small>Alvo {item.goodAt}+ | {diagnosticTitle(item.tone)}</small>
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
            <div className="diagnostic-card">
              <span>Fixing / Tesouros / Rocks</span>
              <strong>
                {analysis.manaBase.fixingSourceCount ?? 0}/
                {analysis.manaBase.treasureSourceCount ?? 0}/
                {analysis.manaBase.manaRockCount ?? 0}
              </strong>
              <small>{analysis.manaBase.fetchLandCount ?? 0} fetches, {analysis.manaBase.conditionalSourceCount ?? 0} fontes condicionais</small>
            </div>
          </div>
          <div className="color-grid">
            {['W', 'U', 'B', 'R', 'G', 'C'].map((color) => (
              <div key={color} className="color-item">
                <span>{color}</span>
                <strong>{analysis.manaBase.colorSources?.[color] ?? 0}</strong>
                <small>custo {analysis.manaBase.colorCosts?.[color] ?? 0} / pips {analysis.manaBase.pipDemand?.[color] ?? 0}</small>
              </div>
            ))}
          </div>
        </>
      )}

      {analysis.cardTags && Object.keys(analysis.cardTags).length > 0 && (
        <>
          <h4>Tags funcionais</h4>
          <div className="gap-grid">
            {Object.entries(analysis.cardTags)
              .sort(([, left], [, right]) => Number(right) - Number(left))
              .slice(0, 12)
              .map(([tag, count]) => (
                <div key={tag} className="gap-item">
                  <span>{tagLabel(tag)}</span>
                  <strong>{count}</strong>
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
          <p className="analysis-note">
            Fonte: {analysis.combos.source || 'snapshot local'} | Versao: {analysis.combos.version || 'desconhecida'} | Atualizado em: {analysis.combos.updatedAt || 'desconhecido'}
          </p>
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

      <h4>Curva de mana</h4>
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

function tagLabel(tag) {
  return {
    draw: 'Compra',
    'impulse-draw': 'Impulso',
    ramp: 'Ramp',
    fixing: 'Fixing',
    treasure: 'Tesouros',
    'mana-rock': 'Rochas',
    'fetch-land': 'Fetches',
    removal: 'Remocao',
    'stack-interaction': 'Pilha',
    protection: 'Protecao',
    token: 'Tokens',
    sacrifice: 'Sacrificio',
    'sacrifice-outlet': 'Outlet de sacrificio',
    graveyard: 'Cemiterio',
    recursion: 'Recursao',
    'self-mill': 'Self-mill',
    combat: 'Combate',
    trample: 'Atropelar',
    haste: 'Haste',
    'big-creature': 'Criaturas grandes',
  }[tag] || tag
}

function scoreTone(value) {
  if (Number(value) >= 70) return 'good'
  if (Number(value) >= 45) return 'warning'
  return 'bad'
}

function diagnosticTitle(tone) {
  if (tone === 'good') return 'Saudavel'
  if (tone === 'warning') return 'Atencao'
  return 'Critico'
}
