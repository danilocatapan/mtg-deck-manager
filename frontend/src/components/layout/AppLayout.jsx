import React from 'react'

export default function AppLayout({ children }) {
  return (
    <div className="app-root">
      <header className="app-header">
        <div className="container">
          <h1>MTG Deck Manager</h1>
        </div>
      </header>
      <main className="container app-main">{children}</main>
    </div>
  )
}
