import { useEffect, useRef, useState } from 'react'
import Home from './pages/Home'
import Layout from './components/Layout'
import ContactPage from './pages/ContactPage'
import ReleaseNotesPage from './pages/ReleaseNotesPage'
import MetaAdminPage from './pages/MetaAdminPage'
import { HASH_ROUTES, pageViewForRoute, parseHashRoute, routeToHash, titleForRoute } from './services/hashRoutes'

function App() {
  const [route, setRoute] = useState(() => parseHashRoute(window.location.hash))
  const firstViewSync = useRef(true)
  const view = pageViewForRoute(route)

  useEffect(() => {
    document.title = titleForRoute(route)
    if (firstViewSync.current && route.name === HASH_ROUTES.HOME) {
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
  }, [route, view])

  useEffect(() => {
    function syncHashRoute() {
      setRoute(parseHashRoute(window.location.hash))
    }

    window.addEventListener('hashchange', syncHashRoute)
    return () => window.removeEventListener('hashchange', syncHashRoute)
  }, [])

  function navigate(routeValue) {
    const nextHash = routeToHash(routeValue)
    if (nextHash) {
      if (window.location.hash !== nextHash) window.location.hash = nextHash
    } else {
      window.history.pushState('', document.title, window.location.pathname + window.location.search)
    }
    setRoute(routeValue)
  }

  function openReleaseNotes() {
    navigate({ name: HASH_ROUTES.RELEASE_NOTES })
  }

  function openContact() {
    navigate({ name: HASH_ROUTES.CONTACT })
  }

  function openMetaAdmin() {
    navigate({ name: HASH_ROUTES.META_ADMIN })
  }

  function backHome() {
    navigate({ name: HASH_ROUTES.HOME })
  }

  return (
    <Layout onOpenReleaseNotes={openReleaseNotes} onOpenContact={openContact} onOpenMetaAdmin={openMetaAdmin}>
      {view === 'release-notes' && <ReleaseNotesPage onBack={backHome} />}
      {view === 'contact' && <ContactPage onBack={backHome} />}
      {view === 'meta-admin' && <MetaAdminPage onBack={backHome} />}
      {view === 'home' && <Home route={route} onNavigate={navigate} />}
    </Layout>
  )
}

export default App
