import React, { useState } from 'react'
import { importDeck } from '../services/api'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'

export default function ImportDeckPage({ onDone }) {
  const [name, setName] = useState('')
  const [commander, setCommander] = useState('')
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(false)

  const handleFile = async (file) => {
    const text = await file.text()
    setContent(text)
  }

  const handleSubmit = async () => {
    try {
      if (!name || !commander || !content) {
        alert('Name, commander and content are required')
        return
      }
      setLoading(true)
      const created = await importDeck({ name, commander, content })
      alert('Imported deck: ' + created.name)
      onDone()
    } catch (e) {
      alert('Import failed: ' + e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main>
      <h1>Import Deck</h1>
      <Card>
        <div style={{ display: 'grid', gap: 8 }}>
          <label>
            Deck name
            <input value={name} onChange={(e) => setName(e.target.value)} />
          </label>
          <label>
            Commander
            <input value={commander} onChange={(e) => setCommander(e.target.value)} />
          </label>
          <label>
            Paste deck list (one line per card, "qty name")
            <textarea rows={12} value={content} onChange={(e) => setContent(e.target.value)} />
          </label>
          <label>
            Or upload .txt file
            <input type="file" accept=".txt" onChange={(e) => e.target.files && handleFile(e.target.files[0])} />
          </label>
          <div>
            <Button onClick={handleSubmit} disabled={loading}>{loading ? 'Importing...' : 'Import'}</Button>
          </div>
        </div>
      </Card>
    </main>
  )
}
