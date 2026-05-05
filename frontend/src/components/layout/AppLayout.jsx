import React from 'react'
import Logo from '../Logo'

export default function AppLayout({ children }) {
  return (
    <div className="app-root">
      <header className="app-header">
        <div className="container">
          <Logo />
        </div>
      </header>
      <main className="container app-main">{children}</main>
    </div>
  )
}
