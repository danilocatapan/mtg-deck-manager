import { useEffect, useRef, useState } from 'react'
import Home from './pages/Home'
import Layout from './components/Layout'
import ContactPage from './pages/ContactPage'
import ReleaseNotesPage from './pages/ReleaseNotesPage'
import MetaTopDeckAdminPage from './pages/MetaTopDeckAdminPage'

function App() {
  const [view, setView] = useState(() => viewFromHash(window.location.hash))
  const firstViewSync = useRef(true)

  useEffect(() => {
    const titles = {
      home: 'Biblioteca de Decks - MTG Deck Manager',
      'release-notes': 'Novidades - MTG Deck Manager',
      contact: 'Contato - MTG Deck Manager',
      'meta-admin': 'Meta Admin - MTG Deck Manager',
    }
    document.title = titles[view] || titles.home
    if (firstViewSync.current && view === 'home') {
      firstViewSync.current = false
      return
    }
    firstViewSync.current = false
    queueMicrotask(() => {
      const heading = document.querySelector('.app-main h1')
      if (heading instanceof HTMLElement) {
        heading.setAttribute('tabindex', '-1')
        heading.focus({ preventScroll: true })
      }
    })
  }, [view])

  useEffect(() => {
    function syncHashView() {
      setView(viewFromHash(window.location.hash))
    }

    window.addEventListener('hashchange', syncHashView)
    return () => window.removeEventListener('hashchange', syncHashView)
  }, [])

  function openReleaseNotes() {
    window.location.hash = 'release-notes'
    setView('release-notes')
  }

  function openContact() {
    window.location.hash = 'contact'
    setView('contact')
  }

  function openMetaAdmin() {
    window.location.hash = 'meta-admin'
    setView('meta-admin')
  }

function backHome() {
    window.history.pushState('', document.title, window.location.pathname + window.location.search)
    setView('home')
  }

  return (
    <Layout onOpenReleaseNotes={openReleaseNotes} onOpenContact={openContact} onOpenMetaAdmin={openMetaAdmin}>
      {view === 'release-notes' && <ReleaseNotesPage onBack={backHome} />}
      {view === 'contact' && <ContactPage onBack={backHome} />}
      {view === 'meta-admin' && <MetaTopDeckAdminPage onBack={backHome} />}
      {view === 'home' && <Home />}
    </Layout>
  )
}

function viewFromHash(hash) {
  if (hash === '#release-notes') return 'release-notes'
  if (hash === '#contact') return 'contact'
  if (hash === '#meta-admin') return 'meta-admin'
  return 'home'
}

export default App
