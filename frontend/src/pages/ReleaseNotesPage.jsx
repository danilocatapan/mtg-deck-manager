import { useEffect, useState } from 'react'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import StateMessage from '../components/ui/StateMessage'
import ReleaseNoteCard from '../components/release-notes/ReleaseNoteCard'
import { getReleaseNotes } from '../services/releaseNotesApi'

export default function ReleaseNotesPage({ onBack }) {
  const [notes, setNotes] = useState([])
  const [status, setStatus] = useState('loading')

  useEffect(() => {
    let active = true
    getReleaseNotes()
      .then((loadedNotes) => {
        if (!active) return
        setNotes(loadedNotes)
        setStatus('ready')
      })
      .catch((error) => {
        if (!active) return
        console.error('getReleaseNotes error', error)
        setStatus('error')
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <main>
      <section className="zone zone-command page-heading">
        <div>
          <p className="eyebrow">Release Notes</p>
          <h1>Novidades</h1>
          <p className="page-description">Mudancas recentes do MTG Deck Manager, organizadas por versao.</p>
        </div>
        <div className="actions-row">
          <Button variant="secondary" onClick={onBack}>Voltar</Button>
        </div>
      </section>

      {status === 'loading' && (
        <Card><div className="loading">Carregando novidades...</div></Card>
      )}

      {status === 'error' && (
        <StateMessage tone="error" title="Novidades indisponiveis">
          Nao foi possivel carregar as novidades agora.
        </StateMessage>
      )}

      {status === 'ready' && notes.length === 0 && (
        <StateMessage tone="neutral" title="Nenhuma nota de versao">
          Nenhuma nota de versao disponivel.
        </StateMessage>
      )}

      {status === 'ready' && notes.length > 0 && (
        <div className="release-note-list">
          {notes.map((note) => (
            <ReleaseNoteCard key={`${note.version}-${note.date}`} note={note} />
          ))}
        </div>
      )}
    </main>
  )
}
