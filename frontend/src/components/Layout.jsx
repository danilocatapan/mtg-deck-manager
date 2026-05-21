import { useEffect, useState } from 'react'
import Logo from './Logo'
import AuthStatus from './AuthStatus'
import AppFooter from './AppFooter'
import { getAuthProfile, isMetaAdmin, subscribeAuth } from '../services/auth'

export default function Layout({ children, onOpenReleaseNotes, onOpenContact, onOpenMetaAdmin }) {
  const [profile, setProfile] = useState(() => getAuthProfile())
  const canOpenMetaAdmin = isMetaAdmin(profile)

  useEffect(() => subscribeAuth(() => setProfile(getAuthProfile())), [])

  return (
    <div className="app-root">
      <header className="app-header">
        <div className="container">
          <Logo />
          <div className="header-actions">
            {canOpenMetaAdmin && (
              <button type="button" className="header-link" onClick={onOpenMetaAdmin}>
                Meta Admin
              </button>
            )}
            <AuthStatus />
          </div>
        </div>
      </header>
      <main className="container app-main">{children}</main>
      <AppFooter onOpenReleaseNotes={onOpenReleaseNotes} onOpenContact={onOpenContact} />
    </div>
  )
}
