import { useEffect, useState } from 'react'
import { getAppInfo } from '../services/api'
import { FRONTEND_INFO } from '../services/appBuildInfo'

export default function AboutDialog({ open, onClose, onOpenReleaseNotes }) {
  const [apiInfo, setApiInfo] = useState(null)
  const [status, setStatus] = useState('idle')

  useEffect(() => {
    if (!open || status === 'loading' || apiInfo) return

    let active = true
    getAppInfo()
      .then((info) => {
        if (!active) return
        setApiInfo(info)
        setStatus('ready')
      })
      .catch(() => {
        if (!active) return
        console.error('getAppInfo error')
        setStatus('error')
      })

    return () => {
      active = false
    }
  }, [apiInfo, open, status])

  if (!open) return null

  const objective = apiInfo?.objective || 'Análise e otimização de decks Commander com recomendações explicáveis.'
  const creator = apiInfo?.creator || 'Danilo Catapan'

  return (
    <div className="about-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="about-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="about-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="about-header">
          <div>
            <p className="eyebrow">Sobre</p>
            <h2 id="about-title">MTG Deck Manager</h2>
          </div>
          <button className="btn secondary about-close" type="button" onClick={onClose} aria-label="Fechar sobre">
            Fechar
          </button>
        </div>

        <p className="about-objective">{objective}</p>

        <div className="about-actions">
          <span>Versão {FRONTEND_INFO.version}</span>
          <button className="about-link" type="button" onClick={onOpenReleaseNotes}>
            Ver novidades
          </button>
        </div>

        <div className="about-grid">
          <InfoBlock
            title="Produto"
            rows={[
              ['Nome', FRONTEND_INFO.name],
              ['Criador', creator],
              ['Ambiente', FRONTEND_INFO.environment],
            ]}
          />
          <InfoBlock
            title="Frontend"
            rows={[
              ['Versao', `v${FRONTEND_INFO.version}`],
              ['Commit', FRONTEND_INFO.commit],
              ['Branch', FRONTEND_INFO.branch],
              ['Build', FRONTEND_INFO.buildTime],
            ]}
          />
          <InfoBlock title="API" rows={apiRows(apiInfo, status === 'idle' ? 'loading' : status)} />
        </div>
      </section>
    </div>
  )
}

function InfoBlock({ title, rows }) {
  return (
    <article className="about-block">
      <h3>{title}</h3>
      <dl>
        {rows.map(([label, value]) => (
          <div key={label}>
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>
    </article>
  )
}

function apiRows(apiInfo, status) {
  if (status === 'loading') {
    return [['Status', 'Carregando']]
  }

  if (status === 'error') {
    return [['Status', 'API indisponível']]
  }

  return [
    ['Nome', apiInfo?.name || 'MTG Deck Manager API'],
    ['Versão', `v${apiInfo?.version || 'desconhecida'}`],
    ['Commit', shortCommit(apiInfo?.commit || 'desconhecido')],
    ['Branch', apiInfo?.branch || 'desconhecida'],
    ['Build', apiInfo?.buildTime || 'desconhecido'],
    ['Ambiente', apiInfo?.environment || 'desconhecido'],
  ]
}

function shortCommit(commit) {
  return commit && commit !== 'local' ? commit.slice(0, 7) : commit
}
