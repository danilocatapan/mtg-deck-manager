import { useMemo, useState } from 'react'
import CardNamePreview from './CardNamePreview'

const ROLE_COLORS = ['#d6a84f', '#b85c45', '#5aa7c8', '#8fcb6b', '#c59bff', '#f0c86a', '#d98d72']

export default function DeckAnalysis({ analysis }) {
  const [activeTab, setActiveTab] = useState('status')
  const vitals = useMemo(() => buildVitals(analysis), [analysis])
  const roleEntries = useMemo(() => buildRoleEntries(analysis?.roles, analysis?.roleCards), [analysis?.roles, analysis?.roleCards])
  const curveEntries = useMemo(
    () => buildCurveEntries(analysis?.manaCurve, analysis?.manaCurveCards),
    [analysis?.manaCurve, analysis?.manaCurveCards],
  )
  const comboAlert = comboSummary(analysis?.combos)

  if (!analysis) return null

  const tabs = [
    { key: 'status', label: 'Status' },
    { key: 'curve', label: 'Curva' },
    { key: 'roles', label: 'Papeis' },
    { key: 'combos', label: 'Combos' },
  ]

  return (
    <div className="analysis-panel compact-analysis">
      <div className="analysis-tabs" role="tablist" aria-label="Seções da análise">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.key}
            className={activeTab === tab.key ? 'active' : ''}
            onClick={() => setActiveTab(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'status' && (
        <section className="analysis-tab-panel" role="tabpanel">
          <div className="deck-health-summary">
            {vitals.slice(0, 6).map((item) => (
              <article key={item.label} className={`summary-tile metric-${item.tone}`}>
                <span>{item.label}</span>
                <strong>{item.value}</strong>
                <small>{item.summary}</small>
              </article>
            ))}
          </div>
          <div className="deck-verdict-grid">
            <VerdictBlock title="O que esta bom" items={vitals.filter((item) => item.tone === 'good').slice(0, 3)} kind="good" />
            <VerdictBlock title="Ajustar primeiro" items={vitals.filter((item) => item.tone !== 'good').slice(0, 3)} kind="fix" />
          </div>
        </section>
      )}

      {activeTab === 'curve' && (
        <section className="analysis-tab-panel" role="tabpanel">
          <div className="curve-chart" aria-label="Curva de mana">
            {curveEntries.map((entry, index) => (
              <details key={entry.label} className="curve-detail-card" open={index === 0 && entry.cards.length > 0}>
                <summary>
                  <span>{entry.label}</span>
                  <div><i style={{ width: `${entry.percent}%` }} /></div>
                  <strong>{entry.value}</strong>
                </summary>
                {entry.cards.length ? (
                  <div className="curve-card-list">
                    {entry.cards.map((card) => (
                      <div key={`${entry.label}-${card.name}`} className="role-card-row">
                        <CardNamePreview name={card.name} prefix={`${card.quantity || 1}x `} imageUrl={card.imageUrl} />
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="empty-inline">A analise atual nao trouxe cartas detalhadas para este custo.</div>
                )}
              </details>
            ))}
          </div>
        </section>
      )}

      {activeTab === 'roles' && (
        <section className="analysis-tab-panel roles-visual-grid" role="tabpanel">
          <div
            className="role-donut"
            style={{ background: roleDonutGradient(roleEntries) }}
            aria-label="Distribuição dos papéis do deck"
          >
            <strong>{roleEntries.reduce((sum, entry) => sum + entry.value, 0)}</strong>
            <span>sinais</span>
          </div>
          <div className="role-legend">
            {roleEntries.length ? roleEntries.map((entry, index) => (
              <details key={entry.key} className="role-detail-card" open={index === 0}>
                <summary>
                  <i style={{ background: entry.color }} />
                  <span>{entry.label}</span>
                  <strong>{entry.value}</strong>
                  <span className="role-help">
                    <span className="role-help-trigger" tabIndex="0" aria-label={`Ajuda sobre ${entry.label}`}>i</span>
                    <span className="role-help-popover" role="tooltip">{entry.help}</span>
                  </span>
                </summary>
                {entry.cards.length ? (
                  <div className="role-card-list">
                    {entry.cards.map((card) => (
                      <div key={`${entry.key}-${card.name}`} className="role-card-row">
                        <CardNamePreview name={card.name} prefix={`${card.quantity || 1}x `} imageUrl={card.imageUrl} />
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="empty-inline">A analise atual nao trouxe cartas detalhadas para este papel.</div>
                )}
              </details>
            )) : <div className="empty-inline">Rode a análise para ver os papéis classificados.</div>}
          </div>
        </section>
      )}

      {activeTab === 'combos' && (
        <section className="analysis-tab-panel" role="tabpanel">
          {comboAlert ? (
            <section className={`combo-callout ${comboAlert.tone}`}>
              <span>Combos</span>
              <strong>{comboAlert.title}</strong>
              <p>{comboAlert.text}</p>
            </section>
          ) : (
            <div className="empty-inline">Nenhum combo conhecido ou linha a uma carta foi detectado neste snapshot.</div>
          )}
          <ComboList title="Presentes" items={analysis.combos?.present || []} />
          <ComboList title="A uma carta" items={analysis.combos?.oneCardAway || []} nearMiss />
        </section>
      )}
    </div>
  )
}

function VerdictBlock({ title, items, kind }) {
  return (
    <div className={`verdict-block ${kind === 'fix' ? 'priority' : ''}`}>
      <h4>{title}</h4>
      {items.length ? items.map((item) => (
        <p key={item.label}><strong>{item.label}:</strong> {kind === 'fix' ? item.fixText : item.goodText}</p>
      )) : <p>{kind === 'fix' ? 'A estrutura principal parece saudavel para a proxima rodada de ajustes.' : 'O deck ainda precisa de ajustes basicos antes de ter pontos fortes claros.'}</p>}
    </div>
  )
}

function ComboList({ title, items, nearMiss = false }) {
  if (!items.length) return null
  return (
    <div className="combo-section">
      <h4>{title}</h4>
      {items.slice(0, 6).map((combo) => (
        <article key={`${combo.name}-${combo.missingCard || ''}`}>
          <strong><ComboCardNames names={cardNamesForComboTitle(combo)} /></strong>
          {nearMiss && (
            <span>
              Falta: <CardNamePreview name={combo.missingCard} />
            </span>
          )}
          {nearMiss && <small><ComboCardNames names={combo.presentCards || []} /></small>}
          {combo.result && <small>{combo.result}</small>}
        </article>
      ))}
    </div>
  )
}

function ComboCardNames({ names = [] }) {
  const cardNames = names.filter(Boolean)
  if (!cardNames.length) return null
  return cardNames.map((name, index) => (
    <span key={`${name}-${index}`} className="combo-card-name">
      {index > 0 && <span className="combo-card-separator"> + </span>}
      <CardNamePreview name={name} />
    </span>
  ))
}

function cardNamesForComboTitle(combo) {
  const knownCards = combo.cards?.length ? combo.cards : combo.presentCards
  if (knownCards?.length) {
    return combo.missingCard ? [combo.missingCard, ...knownCards] : knownCards
  }
  return String(combo.name || '')
    .split(/\s+\+\s+/)
    .map((name) => name.trim())
    .filter(Boolean)
}

function buildRoleEntries(roles = {}, roleCards = {}) {
  return Object.entries(roles)
    .filter(([, value]) => Number(value) > 0)
    .sort(([, left], [, right]) => Number(right) - Number(left))
    .map(([key, value], index) => ({
      key,
      label: roleLabel(key),
      value: Number(value),
      color: ROLE_COLORS[index % ROLE_COLORS.length],
      help: roleHelp(key),
      cards: [...(roleCards?.[key] || [])].sort((left, right) => String(left.name).localeCompare(String(right.name))),
    }))
}

function buildCurveEntries(manaCurve = {}, manaCurveCards = {}) {
  const grouped = Object.entries(manaCurve).reduce((acc, [key, value]) => {
    const numericKey = Number(key)
    const bucket = numericKey >= 7 ? 7 : numericKey
    acc.set(bucket, (acc.get(bucket) || 0) + (Number(value) || 0))
    return acc
  }, new Map())
  const cardsByBucket = Object.entries(manaCurveCards || {}).reduce((acc, [key, cards]) => {
    const numericKey = Number(key)
    const bucket = numericKey >= 7 ? 7 : numericKey
    acc.set(bucket, [...(acc.get(bucket) || []), ...(cards || [])])
    return acc
  }, new Map())
  const entries = [...grouped.entries()]
    .map(([key, value]) => ({
      label: key >= 7 ? '7+' : String(key),
      value,
      order: key,
      cards: [...(cardsByBucket.get(key) || [])].sort((left, right) => String(left.name).localeCompare(String(right.name))),
    }))
    .sort((left, right) => left.order - right.order)
  const max = Math.max(1, ...entries.map((entry) => entry.value))
  return entries.map((entry) => ({ ...entry, percent: Math.max(4, Math.round((entry.value / max) * 100)) }))
}

function roleDonutGradient(entries) {
  const total = entries.reduce((sum, entry) => sum + entry.value, 0)
  if (!total) return 'conic-gradient(rgba(214, 168, 79, 0.18) 0 100%)'
  let cursor = 0
  const stops = entries.map((entry) => {
    const start = cursor
    cursor += (entry.value / total) * 100
    return `${entry.color} ${start}% ${cursor}%`
  })
  return `conic-gradient(${stops.join(', ')})`
}

function roleLabel(role) {
  const labels = {
    ramp: 'Ramp',
    draw: 'Compra',
    interaction: 'Remocao / Interacao',
    removal: 'Remocao',
    protection: 'Protecao',
    boardWipe: 'Limpa-mesa',
    wincon: 'Vitoria',
    land: 'Terrenos',
  }
  return labels[role] || role
}

function roleHelp(role) {
  const help = {
    ramp: 'Cartas que aceleram mana, geram mana adicional, buscam terrenos ou reduzem custos.',
    draw: 'Cartas que compram cartas, geram vantagem de cartas ou permitem selecao relevante.',
    interaction: 'Cartas que removem criaturas, permanentes ou ameacas relevantes.',
    removal: 'Cartas que removem criaturas, permanentes ou ameacas relevantes.',
    protection: 'Cartas que protegem o comandante, criaturas importantes ou o campo contra remocoes.',
    boardWipe: 'Cartas que limpam varias criaturas ou permanentes de uma vez.',
    wincon: 'Cartas que ajudam a fechar o jogo ou transformar vantagem em vitoria.',
    land: 'Terrenos usados para montar a base de mana do deck.',
  }
  return help[role] || 'Cartas classificadas por sinais de texto, tipo e papel estrutural na analise.'
}

function buildVitals(analysis) {
  if (!analysis) return []

  const landCount = Number(analysis.manaBase?.landCount ?? 0)
  const tappedLands = Number(analysis.manaBase?.tappedLandCount ?? 0)
  const averageCmc = Number(analysis.averageCmc ?? 0)
  const ramp = Number(analysis.rampCount ?? 0)
  const draw = Number(analysis.drawCount ?? 0)
  const interaction = Number(analysis.removalCount ?? 0)

  return [
    {
      label: 'Tamanho',
      value: `${analysis.totalCards ?? 0}/99`,
      tone: analysis.totalCards === 99 ? 'good' : analysis.totalCards >= 90 ? 'warning' : 'bad',
      summary: analysis.totalCards === 99 ? 'lista Commander completa' : 'deck ainda incompleto',
      goodText: 'a lista principal esta no tamanho certo para Commander.',
      fixText: 'complete a lista ate 99 cartas antes de avaliar upgrades finos.',
    },
    {
      label: 'Mana base',
      value: `${landCount} terrenos`,
      tone: landCount >= 34 && landCount <= 38 && tappedLands <= 12 ? 'good' : landCount >= 31 && landCount <= 40 ? 'warning' : 'bad',
      summary: `${tappedLands} entram virados`,
      goodText: 'a quantidade de terrenos parece dentro da faixa comum de Commander.',
      fixText: landCount < 34 ? 'adicione terrenos ou fontes consistentes para evitar mulligans ruins.' : 'revise excesso de terrenos ou terrenos lentos.',
    },
    {
      label: 'Curva',
      value: Number.isFinite(averageCmc) ? averageCmc.toFixed(2) : '-',
      tone: averageCmc > 0 && averageCmc <= 3.4 ? 'good' : averageCmc <= 4.0 ? 'warning' : 'bad',
      summary: 'CMC medio',
      goodText: 'a curva deve deixar o deck jogar antes de ficar atras da mesa.',
      fixText: 'corte cartas caras de baixo impacto e aumente jogadas de custo 1-3.',
    },
    {
      label: 'Ramp',
      value: String(ramp),
      tone: ramp >= 10 ? 'good' : ramp >= 7 ? 'warning' : 'bad',
      summary: 'fontes de aceleracao',
      goodText: 'ha aceleracao suficiente para executar o plano com regularidade.',
      fixText: 'adicione ramp barato, rocks ou buscas de terreno alinhadas as cores.',
    },
    {
      label: 'Compra',
      value: String(draw),
      tone: draw >= 8 ? 'good' : draw >= 5 ? 'warning' : 'bad',
      summary: 'fontes de card advantage',
      goodText: 'o deck tem meios razoaveis de recuperar recursos.',
      fixText: 'inclua compra, selecao ou motores de valor para nao ficar sem cartas.',
    },
    {
      label: 'Interacao',
      value: String(interaction),
      tone: interaction >= 8 ? 'good' : interaction >= 5 ? 'warning' : 'bad',
      summary: 'respostas/remocoes',
      goodText: 'o deck tem respostas para impedir planos adversarios.',
      fixText: 'adicione remocoes flexiveis, protecao ou interacao de pilha conforme o bracket.',
    },
  ]
}

function comboSummary(combos) {
  const present = combos?.present?.length || 0
  const near = combos?.oneCardAway?.length || 0
  if (present > 0) {
    return {
      tone: 'good',
      title: `${present} combo${present > 1 ? 's' : ''} detectado${present > 1 ? 's' : ''}`,
      text: 'O deck ja tem linhas conhecidas de fechamento. Confirme se elas combinam com a proposta da mesa.',
    }
  }
  if (near > 0) {
    return {
      tone: 'warning',
      title: `${near} linha${near > 1 ? 's' : ''} a uma carta`,
      text: 'Pode valer buscar a peca faltante se o objetivo for aumentar consistencia de vitoria.',
    }
  }
  return null
}
