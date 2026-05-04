import React, { useEffect, useState } from 'react'
import { fetchDecks } from '../services/api'
import DeckList from '../components/DeckList'
import CardSearch from '../components/CardSearch'

export default function Home() {
  const [decks, setDecks] = useState([])

  useEffect(() => {
    let mounted = true
    fetchDecks().then((d) => {
      if (mounted) setDecks(d)
    })
    return () => (mounted = false)
  }, [])

  return (
    <main style={{ padding: 20 }}>
      <h1>Decks</h1>
      <DeckList decks={decks} />

      <h2 style={{ marginTop: 40 }}>Search Cards</h2>
      <CardSearch />
    </main>
  )
}
