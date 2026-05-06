import Logo from './Logo'
import AuthStatus from './AuthStatus'

export default function Layout({ children }) {
  return (
    <div className="app-root">
      <header className="app-header">
        <div className="container">
          <Logo />
          <AuthStatus />
        </div>
      </header>
      <main className="container app-main">{children}</main>
    </div>
  )
}
