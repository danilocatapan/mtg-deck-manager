import { useState } from 'react'
import AboutDialog from './AboutDialog'
import { FRONTEND_INFO } from '../services/appBuildInfo'

export default function AppFooter({ onOpenReleaseNotes }) {
  const [aboutOpen, setAboutOpen] = useState(false)

  return (
    <>
      <footer className="app-footer">
        <div className="container app-footer-inner">
          <span>{FRONTEND_INFO.name} v{FRONTEND_INFO.version}</span>
          <button className="about-link" type="button" onClick={() => setAboutOpen(true)}>
            Sobre
          </button>
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
