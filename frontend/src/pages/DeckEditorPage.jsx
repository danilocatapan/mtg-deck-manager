import React, { useEffect, useMemo, useState } from 'react'
import DeckForm from '../components/DeckForm'
import DeckAnalysis from '../components/DeckAnalysis'
import Recommendations from '../components/Recommendations'
import RecommendationForm from '../components/RecommendationForm'
import { createDeck, getDeckAnalysis, getRecommendations, updateDeck } from '../services/api'
import Button from '../components/ui/Button'
import analyzeIcon from '../assets/icons/analyze.png'
import recommendIcon from '../assets/icons/recommend.png'

export default function DeckEditorPage({ mode = 'create', deck = null, onDone }) {
  const [initial, setInitial] = useState(null)
  const [analysis, setAnalysis] = useState(null)
  const [rec, setRec] = useState(null)
  const [loadingAnalysis, setLoadingAnalysis] = useState(false)
  const [loadingRec, setLoadingRec] = useState(false)
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)
  const [activePanel, setActivePanel] = useState('editor')

  useEffect(() => {
    if (mode === 'edit' && deck) setInitial(deck)
    if (mode === 'create') setInitial(null)
  }, [mode, deck])

  const savedCardCount = useMemo(() => deck?.cards?.reduce((sum, card) => sum + Number(card.quantity || 0), 0) ?? 0, [deck])
  const canAnalyze = mode === 'edit' && deck?.id && savedCardCount > 0 && savedCardCount <= 99

  async function handleSave(payload) {
    try {
      setError(null)
      if (mode === 'create') {
        const created = await createDeck(payload)
        console.log('Deck created', created)
        onDone && onDone(`Created ${created.name}. Open it to analyze and optimize.`)
      } else {
        const updated = await updateDeck(deck.id, payload)
        console.log('Deck updated', updated)
        onDone && onDone(`Saved ${updated.name}.`)
      }
    } catch (e) {
      console.error('save error', e)
      setError(e.message || 'Failed to save deck.')
    }
  }

  async function handleAnalyze() {
    if (!canAnalyze) return
    try {
      setError(null)
      setLoadingAnalysis(true)
      const deckAnalysis = await getDeckAnalysis(deck.id)
      console.log('analysis', deckAnalysis)
      setAnalysis(deckAnalysis)
      setActivePanel('analysis')
      setMessage('Analysis updated.')
    } catch (e) {
      console.error('analysis error', e)
      setError(e.message || 'Failed to get analysis.')
    } finally {
      setLoadingAnalysis(false)
    }
  }

  async function handleRecommend(params) {
    if (!canAnalyze) return
    try {
      setError(null)
      setLoadingRec(true)
      const recommendations = await getRecommendations(deck.id, params)
      console.log('recommendations', recommendations)
      setRec(recommendations)
      setActivePanel('recommendations')
      setMessage('Recommendations generated.')
    } catch (e) {
      console.error('recommendations error', e)
      setError(e.message || 'Failed to get recommendations.')
    } finally {
      setLoadingRec(false)
    }
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">{mode === 'create' ? 'Step 1 of 4' : 'Deck workspace'}</p>
          <h1>{mode === 'create' ? 'Create Deck' : deck?.name || 'Edit Deck'}</h1>
          <p className="page-description">
            {mode === 'create'
              ? 'Build the list first. After saving, reopen the deck to analyze and request recommendations.'
              : 'Edit the list, analyze deck structure, then generate explainable recommendations.'}
          </p>
        </div>
        <Button variant="secondary" onClick={() => onDone && onDone()}>Back to Decks</Button>
      </div>

      <div className="tabs" role="tablist" aria-label="Deck workflow">
        <button type="button" className={activePanel === 'editor' ? 'active' : ''} onClick={() => setActivePanel('editor')}>Editor</button>
        <button type="button" className={activePanel === 'analysis' ? 'active' : ''} onClick={() => setActivePanel('analysis')} disabled={!analysis}>Analysis</button>
        <button type="button" className={activePanel === 'recommendations' ? 'active' : ''} onClick={() => setActivePanel('recommendations')} disabled={!rec}>Recommendations</button>
      </div>

      {message && <div className="status success">{message}</div>}
      {error && <div className="status error">{error}</div>}

      {activePanel === 'editor' && (
        <div className="card">
          <DeckForm initial={initial} onCancel={() => onDone && onDone()} onSave={handleSave} />
        </div>
      )}

      {activePanel === 'analysis' && (
        <div className="card">
          <div className="section-heading">
            <div>
              <h2>Analysis</h2>
              <p>Structural metrics that guide deck decisions.</p>
            </div>
            <Button onClick={handleAnalyze} disabled={!canAnalyze || loadingAnalysis}>
              <img className="btn-icon" src={analyzeIcon} alt="" aria-hidden="true" />
              {loadingAnalysis ? 'Analyzing...' : 'Refresh Analysis'}
            </Button>
          </div>
          {analysis ? <DeckAnalysis analysis={analysis} /> : <div className="empty-inline">Run analysis from the editor actions first.</div>}
        </div>
      )}

      {activePanel === 'recommendations' && (
        <div className="card">
          <div className="section-heading">
            <div>
              <h2>Recommendations</h2>
              <p>Suggestions explain what gap each card is trying to solve.</p>
            </div>
          </div>
          <RecommendationForm onSubmit={handleRecommend} disabled={!canAnalyze || loadingRec} />
          {loadingRec && <div className="loading">Loading recommendations...</div>}
          {rec ? <Recommendations rec={rec} /> : <div className="empty-inline">Generate recommendations after saving a valid deck.</div>}
        </div>
      )}

      <div className="card action-card">
        <div>
          <h3>Next step</h3>
          <p>{canAnalyze ? 'This saved deck can be analyzed and optimized.' : 'Save a valid deck with up to 99 cards before running analysis.'}</p>
        </div>
        <div className="actions-row">
          <Button onClick={handleAnalyze} disabled={!canAnalyze || loadingAnalysis}>
            <img className="btn-icon" src={analyzeIcon} alt="" aria-hidden="true" />
            {loadingAnalysis ? 'Analyzing...' : 'Analyze Deck'}
          </Button>
          <Button variant="secondary" onClick={() => setActivePanel('recommendations')} disabled={!canAnalyze}>
            <img className="btn-icon" src={recommendIcon} alt="" aria-hidden="true" />
            Open Recommendations
          </Button>
        </div>
      </div>
    </section>
  )
}
