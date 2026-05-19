import { useState } from 'react'
import AboutDialog from './AboutDialog'
import { FRONTEND_INFO } from '../services/appBuildInfo'

export default function AppFooter({ onOpenReleaseNotes, onOpenContact }) {
  const [aboutOpen, setAboutOpen] = useState(false)

  return (
    <>
      <footer className="app-footer">
        <div className="container app-footer-inner">
          <span>{FRONTEND_INFO.name} v{FRONTEND_INFO.version}</span>
          <nav className="app-footer-actions" aria-label="Links do projeto">
            <button className="about-link footer-action" type="button" onClick={onOpenContact}>
              Contato
            </button>
            <a className="about-link footer-action" href={`${import.meta.env.BASE_URL}privacy-policy.html`}>
              Privacidade
            </a>
            <button className="about-link footer-action" type="button" onClick={() => setAboutOpen(true)}>
              Sobre
            </button>
          </nav>
        </div>
      </footer>
      <AboutDialog
        open={aboutOpen}
        onClose={() => setAboutOpen(false)}
        onOpenReleaseNotes={() => {
          setAboutOpen(false)
          onOpenReleaseNotes()
        }}
      />
    </>
  )
}
