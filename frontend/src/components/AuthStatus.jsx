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
          setError(e.message || 'Google login failed.')
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
    return <div className="auth-status auth-warning">Set VITE_GOOGLE_CLIENT_ID to enable login.</div>
  }

  if (profile) {
    return (
      <div className="auth-status">
        <span>{profile.email || profile.name || 'Signed in'}</span>
        <Button variant="secondary" onClick={clearAuthToken}>Sign out</Button>
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
