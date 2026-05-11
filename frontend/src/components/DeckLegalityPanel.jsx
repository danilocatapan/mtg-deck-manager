import StateMessage from './ui/StateMessage'

export default function DeckLegalityPanel({ legality, loading = false, error = null }) {
  if (loading) {
    return (
      <section className="legality-panel compact" aria-label="Legalidade Commander">
        <details open>
          <summary>
            <span>
              <span className="eyebrow">Regras Commander</span>
              <strong>Verificando legalidade...</strong>
            </span>
            <span className="status-pill">Validando</span>
          </summary>
          <StateMessage title="Verificando legalidade Commander...">Validando tamanho, cores, singleton e banlist.</StateMessage>
        </details>
      </section>
    )
  }

  if (error) {
    return (
      <section className="legality-panel compact" aria-label="Legalidade Commander">
        <details open>
          <summary>
            <span>
              <span className="eyebrow">Regras Commander</span>
              <strong>Legalidade indisponivel</strong>
            </span>
            <span className="status-pill danger">Revisar</span>
          </summary>
          <StateMessage tone="error" title="Nao foi possivel verificar a legalidade">Tente salvar novamente ou recarregar o deck.</StateMessage>
        </details>
      </section>
    )
  }

  if (!legality) {
    return (
      <section className="legality-panel compact" aria-label="Legalidade Commander">
        <details open>
          <summary>
            <span>
              <span className="eyebrow">Regras Commander</span>
              <strong>Legalidade ainda nao verificada</strong>
            </span>
            <span className="status-pill">Pendente</span>
          </summary>
          <StateMessage title="Legalidade ainda nao verificada">Salve o deck para validar regras Commander.</StateMessage>
        </details>
      </section>
    )
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
    {
      label: 'Game Changers',
      value: `${legality.gameChangerCount || 0}`,
      ok: Number(legality.gameChangerCount || 0) <= 3 || Number(legality.estimatedBracket?.level || 0) >= 4,
      detail: legality.gameChangerCount ? joinNames(legality.gameChangers) : 'Nenhum Game Changer detectado',
    },
  ]
  const failedChecks = checks.filter((check) => !check.ok).length
  const mainDeckSize = legality.mainDeckSize ?? 0
  const targetMainDeckSize = legality.targetMainDeckSize ?? 99
  const detailsOpen = !legality.legal || failedChecks > 0

  return (
    <section className="legality-panel compact" aria-label="Legalidade Commander">
      <details open={detailsOpen}>
        <summary>
          <span>
            <span className="eyebrow">Regras Commander</span>
            <strong>Legalidade</strong>
          </span>
          <span className="legality-summary-metrics">
            <span>{mainDeckSize}/{targetMainDeckSize}</span>
            <span>{failedChecks ? `${failedChecks} ajuste(s)` : 'Sem bloqueios'}</span>
            <span className={legality.legal ? 'status-pill ready' : 'status-pill danger'}>
              {legality.legal ? 'Legal' : 'Revisar'}
            </span>
          </span>
        </summary>

        <div className="diagnostic-grid">
          {checks.map((check) => (
            <article key={check.label} className={`diagnostic-card metric-${check.ok ? 'good' : 'bad'}`}>
              <div className="diagnostic-card-header">
                <span>{check.label}</span>
                <strong>{check.value}</strong>
              </div>
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

        {legality.rulesSnapshot && (
          <details className="recommendation-source-details">
            <summary>Snapshot de regras</summary>
            <div>
              <span className="source-pill enabled">Banlist: {legality.rulesSnapshot.banlistDate || 'sem data'}</span>
              <span className="source-pill enabled">Game Changers: {legality.rulesSnapshot.gameChangersDate || 'sem data'}</span>
              <span className="source-pill enabled">Bracket: {legality.rulesSnapshot.bracketVersion || 'desconhecido'}</span>
              <span className="source-pill">Scryfall: {legality.rulesSnapshot.scryfallLegalityVersion || 'cache atual'}</span>
            </div>
          </details>
        )}
      </details>
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
