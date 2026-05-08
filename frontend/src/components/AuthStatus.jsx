import { useEffect, useRef, useState } from 'react'
import Button from './ui/Button'
import { clearAuthToken, getAuthProfile, getGoogleClientId, setAuthToken, subscribeAuth } from '../services/auth'

export default function AuthStatus() {
  const buttonRef = useRef(null)
  const [profile, setProfile] = useState(() => getAuthProfile())
  const [error, setError] = useState(null)
  const clientId = getGoogleClientId()

  useEffect(() => subscribeAuth(() => setProfile(getAuthProfile())), [])

  useEffect(() => {
    if (!clientId || profile || !buttonRef.current) return
    if (!window.google?.accounts?.id) {
      return
    }

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
  }, [clientId, profile])

  if (!clientId) {
    return <div className="auth-status auth-warning">Defina VITE_GOOGLE_CLIENT_ID para habilitar login.</div>
  }

  if (profile) {
    return (
      <div className="auth-status">
        <span>{profile.email || profile.name || 'Logado'}</span>
        <Button variant="secondary" onClick={clearAuthToken}>Sair</Button>
      </div>
    )
  }

  return (
    <div className="auth-status">
      <div ref={buttonRef} />
      {error && <span className="auth-warning">{error}</span>}
    </div>
  )
}
