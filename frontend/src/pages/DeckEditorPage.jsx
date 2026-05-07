import { useEffect, useMemo, useState } from 'react'
import DeckForm from '../components/DeckForm'
import DeckAnalysis from '../components/DeckAnalysis'
import RecommendationPanel from '../components/recommendations/RecommendationPanel'
import RecommendationSettings from '../components/recommendations/RecommendationSettings'
import { applyRecommendationSwap, createDeck, getCommanderMeta, getDeckAnalysis, getMetaSources, getRecommendations, updateDeck } from '../services/api'
import Button from '../components/ui/Button'
import analyzeIcon from '../assets/icons/analyze.png'
import recommendIcon from '../assets/icons/recommend.png'

export default function DeckEditorPage({ mode = 'create', deck = null, onDone }) {
  const [analysis, setAnalysis] = useState(null)
  const [rec, setRec] = useState(null)
  const [metaProfile, setMetaProfile] = useState(null)
  const [metaSources, setMetaSources] = useState([])
  const [recommendationParams, setRecommendationParams] = useState({ bracket: 'casual' })
  const [loadingAnalysis, setLoadingAnalysis] = useState(false)
  const [loadingRec, setLoadingRec] = useState(false)
  const [recommendationError, setRecommendationError] = useState(null)
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)
  const [activePanel, setActivePanel] = useState('editor')
  const [currentDeck, setCurrentDeck] = useState(deck)
  const [applyingSwapKey, setApplyingSwapKey] = useState(null)
  const [appliedSwapKeys, setAppliedSwapKeys] = useState(() => new Set())

  const initial = mode === 'edit' ? currentDeck : null
  const savedCardCount = useMemo(() => currentDeck?.cards?.reduce((sum, card) => sum + Number(card.quantity || 0), 0) ?? 0, [currentDeck])
  const canAnalyze = mode === 'edit' && currentDeck?.id && savedCardCount > 0 && savedCardCount <= 99

  useEffect(() => {
    if (!canAnalyze) return
    getMetaSources().then(setMetaSources)
  }, [canAnalyze])

  const steps = [
    { key: 'editor', label: 'Editor', state: activePanel === 'editor' ? 'active' : 'complete' },
    { key: 'analysis', label: 'Analysis', state: activePanel === 'analysis' ? 'active' : analysis ? 'complete' : 'locked' },
    { key: 'recommendations', label: 'Recommendations', state: activePanel === 'recommendations' ? 'active' : rec ? 'complete' : 'locked' },
  ]

  async function handleSave(payload) {
    try {
      setError(null)
      if (mode === 'create') {
        const created = await createDeck(payload)
        console.log('Deck created', created)
        onDone && onDone(`Created ${created.name}. Open it to analyze and optimize.`)
      } else {
        const updated = await updateDeck(currentDeck.id, payload)
        setCurrentDeck(updated)
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
      const deckAnalysis = await getDeckAnalysis(currentDeck.id)
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
      setRecommendationError(null)
      setLoadingRec(true)
      setRecommendationParams(params)
      console.info('event=recommendation.request.started', { deckId: currentDeck.id, bracket: params?.bracket || 'casual' })
      const recommendations = await getRecommendations(currentDeck.id, params)
      const profile = await getCommanderMeta(currentDeck.commander, {
        bracket: params?.bracket || 'casual',
      })
      console.log('recommendations', recommendations)
      console.info('event=recommendation.request.completed', { deckId: currentDeck.id, count: Array.isArray(recommendations) ? recommendations.length : 0 })
      if (!profile || Number(profile.sampleSize || 0) < 3) {
        console.info('event=recommendation.fallback.rendered', { deckId: currentDeck.id })
      }
      setRec(recommendations)
      setMetaProfile(profile)
      setActivePanel('recommendations')
      setMessage(`${Array.isArray(recommendations) ? recommendations.length : 0} strategic recommendations generated.`)
    } catch (e) {
      console.error('recommendations error', e)
      console.info('event=recommendation.request.failed', { deckId: currentDeck.id })
      setRecommendationError(e.message || 'Failed to get recommendations.')
      setError(e.message || 'Failed to get recommendations.')
    } finally {
      setLoadingRec(false)
    }
  }

  async function handleApplyRecommendation(item) {
    if (!canAnalyze || !item?.add || !item?.remove) return
    const confirmed = window.confirm(`Aplicar troca: adicionar ${item.add} e remover ${item.remove}?`)
    if (!confirmed) return

    const key = recommendationKey(item)
    try {
      setError(null)
      setRecommendationError(null)
      setApplyingSwapKey(key)
      const updatedDeck = await applyRecommendationSwap(currentDeck.id, {
        add: item.add,
        remove: item.remove,
      })
      setCurrentDeck(updatedDeck)
      setAppliedSwapKeys((previous) => new Set(previous).add(key))
      const deckAnalysis = await getDeckAnalysis(currentDeck.id)
      setAnalysis(deckAnalysis)
      setMessage(`Troca aplicada: ${item.add} entrou no lugar de ${item.remove}.`)
      console.info('event=recommendation.swap.applied', { deckId: currentDeck.id, add: item.add, remove: item.remove })
    } catch (e) {
      console.error('apply recommendation swap error', e)
      setRecommendationError(e.message || 'Nao foi possivel aplicar a troca.')
      setError(e.message || 'Nao foi possivel aplicar a troca.')
    } finally {
      setApplyingSwapKey(null)
    }
  }

  return (
    <section>
      <div className="zone zone-command page-heading">
        <div>
          <p className="eyebrow">Command Zone</p>
          <h1>{mode === 'create' ? 'Create Deck' : currentDeck?.name || 'Edit Deck'}</h1>
          <p className="page-description">
            {mode === 'create'
              ? 'Build the list first. After saving, reopen the deck to analyze and request recommendations.'
              : 'Edit the list, analyze deck structure, then generate explainable recommendations.'}
          </p>
        </div>
        <Button variant="secondary" onClick={() => onDone && onDone()}>Back to Decks</Button>
      </div>

      <div className="workflow-stepper" role="tablist" aria-label="Deck workflow">
        {steps.map((step, index) => (
          <button
            key={step.key}
            type="button"
            className="step"
            data-state={step.state}
            onClick={() => setActivePanel(step.key)}
            disabled={step.state === 'locked'}
          >
            <strong>{index + 1}</strong>
            <span>{step.label}</span>
          </button>
        ))}
      </div>

      {message && <div className="status success">{message}</div>}
      {error && <div className="status error">{error}</div>}

      {activePanel === 'editor' && (
        <div className="card zone zone-library">
          <DeckForm initial={initial} onCancel={() => onDone && onDone()} onSave={handleSave} />
        </div>
      )}

      {activePanel === 'analysis' && (
        <div className="card zone zone-battlefield">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Battlefield Metrics</p>
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
        <div className="card zone zone-sideboard">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Sideboard / Upgrade Path</p>
              <h2>Recommendations</h2>
              <p>Bracket-aware swaps explain the problem, why the add helps, and why the cut is the least painful slot.</p>
            </div>
          </div>
          <RecommendationSettings
            onSubmit={handleRecommend}
            onParamsChange={setRecommendationParams}
            disabled={!canAnalyze || loadingRec}
            loading={loadingRec}
          />
          <RecommendationPanel
            recommendations={rec}
            loading={loadingRec}
            error={recommendationError}
            bracket={recommendationParams.bracket || 'casual'}
            metaProfile={metaProfile}
            metaSources={metaSources}
            onApplyRecommendation={handleApplyRecommendation}
            applyingKey={applyingSwapKey}
            appliedKeys={appliedSwapKeys}
          />
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

function recommendationKey(item) {
  return `${item?.add || ''}|||${item?.remove || ''}`.toLowerCase()
}
