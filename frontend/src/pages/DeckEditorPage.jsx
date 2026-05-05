import React, { useEffect, useState } from 'react'
import DeckForm from '../components/DeckForm'
import DeckAnalysis from '../components/DeckAnalysis'
import Recommendations from '../components/Recommendations'
import RecommendationForm from '../components/RecommendationForm'
import { createDeck, updateDeck, getDeckAnalysis, getRecommendations } from '../services/api'

export default function DeckEditorPage({ mode = 'create', deck = null, onDone }) {
  const [initial, setInitial] = useState(null)

  useEffect(() => {
    if (mode === 'edit' && deck) setInitial(deck)
    if (mode === 'create') setInitial(null)
  }, [mode, deck])

  async function handleSave(payload) {
    try {
      if (mode === 'create') {
        const created = await createDeck(payload)
        console.log('Deck created', created)
      } else {
        const updated = await updateDeck(deck.id, payload)
        console.log('Deck updated', updated)
      }
      onDone && onDone()
    } catch (e) {
      console.error('save error', e)
      alert('Failed to save deck')
    }
  }

  const [analysis, setAnalysis] = useState(null)
  const [rec, setRec] = useState(null)
  const [loadingAnalysis, setLoadingAnalysis] = useState(false)
  const [loadingRec, setLoadingRec] = useState(false)

  async function handleAnalyze() {
    if (!deck && mode === 'edit') return
    const id = deck?.id
    try {
      setLoadingAnalysis(true)
      const a = await getDeckAnalysis(id)
      console.log('analysis', a)
      setAnalysis(a)
    } catch (e) {
      console.error('analysis error', e)
      alert('Failed to get analysis')
    } finally {
      setLoadingAnalysis(false)
    }
  }

  async function handleRecommend(params) {
    if (!deck && mode === 'edit') return
    const id = deck?.id
    try {
      setLoadingRec(true)
      const r = await getRecommendations(id, params)
      console.log('recommendations', r)
      setRec(r)
    } catch (e) {
      console.error('recommendations error', e)
      alert('Failed to get recommendations')
    } finally {
      setLoadingRec(false)
    }
  }

  return (
    <section>
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0 }}>{mode === 'create' ? 'Create Deck' : 'Edit Deck'}</h2>
          <div>
            <button className="btn secondary" onClick={onDone} style={{ marginRight: 8 }}>Back</button>
            <button className="btn" onClick={() => document.querySelector('form')?.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }))}>Save</button>
          </div>
        </div>

        <div style={{ marginTop: 12 }}>
          <DeckForm initial={initial} onCancel={onDone} onSave={handleSave} />
        </div>

        <div style={{ marginTop: 20 }}>
          <button className="btn" onClick={handleAnalyze} disabled={loadingAnalysis || mode !== 'edit'}>
            {loadingAnalysis ? 'Analyzing...' : 'Analyze Deck'}
          </button>
        </div>
      </div>

      {analysis && <div className="card"><DeckAnalysis analysis={analysis} /></div>}

      <div className="card">
        <h3>Recommendations</h3>
        <RecommendationForm onSubmit={handleRecommend} />
        {loadingRec && <div className="loading">Loading recommendations...</div>}
        {rec && <Recommendations rec={rec} />}
      </div>
    </section>
  )
}
