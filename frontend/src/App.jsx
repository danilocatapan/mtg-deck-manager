import { useEffect, useState } from 'react'
import Home from './pages/Home'
import Layout from './components/Layout'
import ReleaseNotesPage from './pages/ReleaseNotesPage'

function App() {
  const [view, setView] = useState(() => window.location.hash === '#release-notes' ? 'release-notes' : 'home')

  useEffect(() => {
    function syncHashView() {
      setView(window.location.hash === '#release-notes' ? 'release-notes' : 'home')
    }

    window.addEventListener('hashchange', syncHashView)
    return () => window.removeEventListener('hashchange', syncHashView)
  }, [])

  function openReleaseNotes() {
    window.location.hash = 'release-notes'
    setView('release-notes')
  }

  function backHome() {
    history.pushState('', document.title, window.location.pathname + window.location.search)
    setView('home')
  }

  return (
    <Layout onOpenReleaseNotes={openReleaseNotes}>
      {view === 'release-notes' ? <ReleaseNotesPage onBack={backHome} /> : <Home />}
    </Layout>
  )
}

export default App
