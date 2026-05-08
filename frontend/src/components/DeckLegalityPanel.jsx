import StateMessage from './ui/StateMessage'

export default function DeckLegalityPanel({ legality, loading = false, error = null }) {
  if (loading) {
    return <StateMessage title="Verificando legalidade Commander...">Validando tamanho, cores, singleton e banlist.</StateMessage>
  }

  if (error) {
    return <StateMessage tone="error" title="Nao foi possivel verificar a legalidade">Tente salvar novamente ou recarregar o deck.</StateMessage>
  }

  if (!legality) {
    return <StateMessage title="Legalidade ainda nao verificada">Salve o deck para validar regras Commander.</StateMessage>
  }

  const checks = [
    {
      label: 'Tamanho',
      value: `${legality.mainDeckSize}/${legality.targetMainDeckSize}`,
      ok: legality.sizeLegal,
      detail: legality.sizeLegal ? 'Deck principal completo' : 'Commander usa 99 cartas fora do comandante',
    },
    {
      label: 'Singleton',
      value: legality.singletonLegal ? 'OK' : `${legality.duplicateCards?.length || 0}`,
      ok: legality.singletonLegal,
      detail: legality.singletonLegal ? 'Sem duplicatas ilegais' : `Duplicadas: ${joinNames(legality.duplicateCards)}`,
    },
    {
      label: 'Identidade de cor',
      value: legality.colorIdentity?.length ? legality.colorIdentity.join('') : 'C',
      ok: legality.colorIdentityLegal,
      detail: legality.colorIdentityLegal ? 'Todas as cartas respeitam as cores' : `Fora da cor: ${joinNames(legality.offColorCards)}`,
    },
    {
      label: 'Banlist',
      value: legality.banlist?.legal ? 'OK' : `${legality.banlist?.bannedCards?.length || 0}`,
      ok: legality.banlist?.legal,
      detail: legality.banlist?.legal ? 'Nenhuma carta banida detectada' : `Banidas: ${joinNames(legality.banlist?.bannedCards)}`,
    },
    {
      label: 'Comandante',
      value: legality.commanderValid ? 'OK' : 'Revisar',
      ok: legality.commanderValid,
      detail: commanderNames(legality.commanders),
    },
    {
      label: 'Companion',
      value: legality.companion?.present ? legality.companion.name : 'Nenhum',
      ok: legality.companion?.legal,
      detail: legality.companion?.reason || 'Nenhum companion declarado',
    },
  ]

  return (
    <section className="legality-panel" aria-label="Legalidade Commander">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Regras Commander</p>
          <h3>Legalidade</h3>
          <p>Validacao do deck principal, comandante, cores, duplicatas e banlist.</p>
        </div>
        <div className={legality.legal ? 'status-pill ready' : 'status-pill danger'}>
          {legality.legal ? 'Legal' : 'Revisar'}
        </div>
      </div>

      <div className="diagnostic-grid">
        {checks.map((check) => (
          <article key={check.label} className={`diagnostic-card metric-${check.ok ? 'good' : 'bad'}`}>
            <span>{check.label}</span>
            <strong>{check.value}</strong>
            <small>{check.detail}</small>
          </article>
        ))}
      </div>

      {legality.estimatedBracket && (
        <div className="state-message">
          <strong>Bracket estimado: {legality.estimatedBracket.level} - {legality.estimatedBracket.label}</strong>
          <span>Estimativa baseada na lista atual; use como apoio para conversa de mesa, nao como nota absoluta.</span>
        </div>
      )}
    </section>
  )
}

function joinNames(names = []) {
  if (!names.length) return 'nenhuma'
  return names.slice(0, 4).join(', ') + (names.length > 4 ? ` +${names.length - 4}` : '')
}

function commanderNames(commanders = []) {
  if (!commanders.length) return 'Comandante nao informado'
  return commanders.map((commander) => `${commander.name} (${commander.role})`).join(', ')
}
