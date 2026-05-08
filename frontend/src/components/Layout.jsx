import Logo from './Logo'
import AuthStatus from './AuthStatus'
import AppFooter from './AppFooter'

export default function Layout({ children, onOpenReleaseNotes }) {
  return (
    <div className="app-root">
      <header className="app-header">
        <div className="container">
          <Logo />
          <AuthStatus />
        </div>
      </header>
      <main className="container app-main">{children}</main>
      <AppFooter onOpenReleaseNotes={onOpenReleaseNotes} />
    </div>
  )
}
