import { useEffect, useState } from 'react'
import Home from './pages/Home'
import Layout from './components/Layout'
import ContactPage from './pages/ContactPage'
import ReleaseNotesPage from './pages/ReleaseNotesPage'

function App() {
  const [view, setView] = useState(() => viewFromHash(window.location.hash))

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

  function backHome() {
    history.pushState('', document.title, window.location.pathname + window.location.search)
    setView('home')
  }

  return (
    <Layout onOpenReleaseNotes={openReleaseNotes} onOpenContact={openContact}>
      {view === 'release-notes' && <ReleaseNotesPage onBack={backHome} />}
      {view === 'contact' && <ContactPage onBack={backHome} />}
      {view === 'home' && <Home />}
    </Layout>
  )
}

function viewFromHash(hash) {
  if (hash === '#release-notes') return 'release-notes'
  if (hash === '#contact') return 'contact'
  return 'home'
}

export default App
