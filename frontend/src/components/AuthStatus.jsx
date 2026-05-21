import { useEffect, useRef, useState } from 'react'
import Button from './ui/Button'
import { clearAuthToken, getAuthProfile, getGoogleClientId, setAuthToken, subscribeAuth } from '../services/auth'

const GOOGLE_BUTTON_RETRY_MS = 250
const GOOGLE_BUTTON_MAX_ATTEMPTS = 80

export default function AuthStatus() {
  const buttonRef = useRef(null)
  const [profile, setProfile] = useState(() => getAuthProfile())
  const [error, setError] = useState(null)
  const [googleReady, setGoogleReady] = useState(() => Boolean(window.google?.accounts?.id))
  const clientId = getGoogleClientId()

  useEffect(() => subscribeAuth(() => setProfile(getAuthProfile())), [])

  useEffect(() => {
    if (!clientId || profile || googleReady) return

    let attempts = 0
    const intervalId = window.setInterval(() => {
      attempts += 1
      if (window.google?.accounts?.id) {
        setGoogleReady(true)
        window.clearInterval(intervalId)
        return
      }
      if (attempts >= GOOGLE_BUTTON_MAX_ATTEMPTS) {
        window.clearInterval(intervalId)
        setError('Login Google demorou para carregar. Recarregue a pagina ou tente novamente em instantes.')
      }
    }, GOOGLE_BUTTON_RETRY_MS)

    return () => window.clearInterval(intervalId)
  }, [clientId, googleReady, profile])

  useEffect(() => {
    if (!clientId || profile || !buttonRef.current || !googleReady) return

    buttonRef.current.innerHTML = ''
    window.google.accounts.id.initialize({
      client_id: clientId,
      callback: (response) => {
        try {
          setAuthToken(response.credential)
          setError(null)
        } catch (e) {
          setError(e.message || 'Falha no login com Google.')
        }
      },
    })
    window.google.accounts.id.renderButton(buttonRef.current, {
      theme: 'outline',
      size: 'medium',
      text: 'signin_with',
      shape: 'pill',
    })
  }, [clientId, googleReady, profile])

  if (!clientId) {
    return <div id="auth-section" className="auth-status auth-warning">Defina VITE_GOOGLE_CLIENT_ID para habilitar login.</div>
  }

  if (profile) {
    return (
      <div id="auth-section" className="auth-status" aria-live="polite">
        <span>{profile.email || profile.name || 'Logado'}</span>
        <Button variant="secondary" onClick={clearAuthToken}>Sair</Button>
      </div>
    )
  }

  return (
    <div id="auth-section" className="auth-status">
      <div ref={buttonRef} />
      {!googleReady && !error && <span className="auth-warning">Carregando login Google...</span>}
      {error && <span className="auth-warning">{error}</span>}
    </div>
  )
}
