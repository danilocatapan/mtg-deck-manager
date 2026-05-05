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
    <section style={{ padding: 20 }}>
      <h2>{mode === 'create' ? 'Create Deck' : 'Edit Deck'}</h2>
      <DeckForm initial={initial} onCancel={onDone} onSave={handleSave} />

      <div style={{ marginTop: 20 }}>
        <button onClick={handleAnalyze} disabled={loadingAnalysis || mode !== 'edit'}>
          {loadingAnalysis ? 'Analyzing...' : 'Analyze Deck'}
        </button>
      </div>

      {analysis && <DeckAnalysis analysis={analysis} />}

      <div style={{ marginTop: 20 }}>
        <h3>Recommendations</h3>
        <RecommendationForm onSubmit={handleRecommend} />
        {loadingRec && <div>Loading recommendations...</div>}
        {rec && <Recommendations rec={rec} />}
      </div>
    </section>
  )
}
