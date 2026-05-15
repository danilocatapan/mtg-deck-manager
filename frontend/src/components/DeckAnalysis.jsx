export default function DeckAnalysis({ analysis }) {
  if (!analysis) return null

  const vitals = buildVitals(analysis)
  const strengths = vitals.filter((item) => item.tone === 'good').slice(0, 3)
  const fixes = vitals.filter((item) => item.tone !== 'good').slice(0, 3)
  const comboAlert = comboSummary(analysis.combos)

  return (
    <div className="analysis-panel compact-analysis">
      <section className="deck-health-summary">
        {vitals.slice(0, 5).map((item) => (
          <article key={item.label} className={`summary-tile metric-${item.tone}`}>
            <span>{item.label}</span>
            <strong>{item.value}</strong>
            <small>{item.summary}</small>
          </article>
        ))}
      </section>

      <section className="deck-verdict-grid">
        <div className="verdict-block">
          <h4>O que está bom</h4>
          {strengths.length ? strengths.map((item) => (
            <p key={item.label}><strong>{item.label}:</strong> {item.goodText}</p>
          )) : <p>O deck ainda precisa de ajustes básicos antes de ter pontos fortes claros.</p>}
        </div>

        <div className="verdict-block priority">
          <h4>Ajustar primeiro</h4>
          {fixes.length ? fixes.map((item) => (
            <p key={item.label}><strong>{item.label}:</strong> {item.fixText}</p>
          )) : <p>A estrutura principal parece saudável. As próximas melhorias podem focar tema, meta e preferência da mesa.</p>}
        </div>
      </section>

      {comboAlert && (
        <section className={`combo-callout ${comboAlert.tone}`}>
          <span>Combos</span>
          <strong>{comboAlert.title}</strong>
          <p>{comboAlert.text}</p>
        </section>
      )}
    </div>
  )
}

function buildVitals(analysis) {
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
      goodText: 'a lista principal está no tamanho certo para Commander.',
      fixText: 'complete a lista até 99 cartas antes de avaliar upgrades finos.',
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
      summary: 'CMC médio',
      goodText: 'a curva deve deixar o deck jogar antes de ficar atrás da mesa.',
      fixText: 'corte cartas caras de baixo impacto e aumente jogadas de custo 1-3.',
    },
    {
      label: 'Ramp',
      value: String(ramp),
      tone: ramp >= 10 ? 'good' : ramp >= 7 ? 'warning' : 'bad',
      summary: 'fontes de aceleração',
      goodText: 'há aceleração suficiente para executar o plano com regularidade.',
      fixText: 'adicione ramp barato, rocks ou buscas de terreno alinhadas às cores.',
    },
    {
      label: 'Compra',
      value: String(draw),
      tone: draw >= 8 ? 'good' : draw >= 5 ? 'warning' : 'bad',
      summary: 'fontes de card advantage',
      goodText: 'o deck tem meios razoáveis de recuperar recursos.',
      fixText: 'inclua compra, seleção ou motores de valor para não ficar sem cartas.',
    },
    {
      label: 'Interação',
      value: String(interaction),
      tone: interaction >= 8 ? 'good' : interaction >= 5 ? 'warning' : 'bad',
      summary: 'respostas/remoções',
      goodText: 'o deck tem respostas para impedir planos adversários.',
      fixText: 'adicione remoções flexíveis, proteção ou interação de pilha conforme o bracket.',
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
      text: 'O deck já tem linhas conhecidas de fechamento. Confirme se elas combinam com a proposta da mesa.',
    }
  }
  if (near > 0) {
    return {
      tone: 'warning',
      title: `${near} linha${near > 1 ? 's' : ''} a uma carta`,
      text: 'Pode valer buscar a peça faltante se o objetivo for aumentar consistência de vitória.',
    }
  }
  return null
}
