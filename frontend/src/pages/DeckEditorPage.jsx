import { useCallback, useEffect, useMemo, useState } from 'react'
import DeckForm from '../components/DeckForm'
import DeckAnalysis from '../components/DeckAnalysis'
import DeckLegalityPanel from '../components/DeckLegalityPanel'
import RecommendationPanel from '../components/recommendations/RecommendationPanel'
import RecommendationSettings from '../components/recommendations/RecommendationSettings'
import {
  addPackageToMaybeboard,
  applyRecommendationSwap,
  createDeck,
  getCommanderMeta,
  getDeckAnalysis,
  getDeckLegality,
  getDeckPackages,
  getMetaSources,
  getRecommendations,
  getSimilarDeckComparison,
  undoRecommendationSwap,
  updateDeck,
} from '../services/api'
import Button from '../components/ui/Button'
import analyzeIcon from '../assets/icons/analyze.png'
import recommendIcon from '../assets/icons/recommend.png'

export default function DeckEditorPage({ mode = 'create', deck = null, onDone }) {
  const [analysis, setAnalysis] = useState(null)
  const [legality, setLegality] = useState(null)
  const [rec, setRec] = useState(null)
  const [metaProfile, setMetaProfile] = useState(null)
  const [metaSources, setMetaSources] = useState([])
  const [comparison, setComparison] = useState(null)
  const [packages, setPackages] = useState([])
  const [recommendationParams, setRecommendationParams] = useState({ bracket: 'casual' })
  const [loadingAnalysis, setLoadingAnalysis] = useState(false)
  const [loadingLegality, setLoadingLegality] = useState(false)
  const [loadingRec, setLoadingRec] = useState(false)
  const [legalityError, setLegalityError] = useState(null)
  const [recommendationError, setRecommendationError] = useState(null)
  const [message, setMessage] = useState(null)
  const [error, setError] = useState(null)
  const [activePanel, setActivePanel] = useState('editor')
  const [currentDeck, setCurrentDeck] = useState(deck)
  const [applyingSwapKey, setApplyingSwapKey] = useState(null)
  const [appliedSwapKeys, setAppliedSwapKeys] = useState(() => new Set())

  const initial = mode === 'edit' ? currentDeck : null
  const savedCardCount = useMemo(() => currentDeck?.cards
    ?.filter((card) => (card.zone || 'main') === 'main')
    .reduce((sum, card) => sum + Number(card.quantity || 0), 0) ?? 0, [currentDeck])
  const canAnalyze = mode === 'edit' && currentDeck?.id && savedCardCount > 0 && savedCardCount <= 99

  const refreshLegality = useCallback(async function refreshDeckLegality(deckId = currentDeck?.id) {
    if (!deckId) return
    try {
      setLoadingLegality(true)
      setLegalityError(null)
      const deckLegality = await getDeckLegality(deckId)
      setLegality(deckLegality)
    } catch (e) {
      console.error('legality error', e)
      setLegalityError(e.message || 'Falha ao verificar legalidade.')
    } finally {
      setLoadingLegality(false)
    }
  }, [currentDeck?.id])

  useEffect(() => {
    if (!canAnalyze) return
    getMetaSources().then(setMetaSources)
  }, [canAnalyze])

  useEffect(() => {
    if (!currentDeck?.id) return
    queueMicrotask(() => refreshLegality(currentDeck.id))
  }, [currentDeck?.id, refreshLegality])

  const visibleAppliedSwapKeys = useMemo(() => {
    const next = new Set(appliedSwapKeys)
    currentDeck?.history?.filter((entry) => !entry.undone).forEach((entry) => next.add(entry.id))
    return next
  }, [appliedSwapKeys, currentDeck?.history])

  const steps = [
    { key: 'editor', label: 'Editor', state: activePanel === 'editor' ? 'active' : 'complete' },
    { key: 'analysis', label: 'Analise', state: activePanel === 'analysis' ? 'active' : analysis ? 'complete' : 'locked' },
    { key: 'recommendations', label: 'Recomendacoes', state: activePanel === 'recommendations' ? 'active' : rec ? 'complete' : 'locked' },
  ]

  async function handleSave(payload) {
    try {
      setError(null)
      if (mode === 'create') {
        const created = await createDeck(payload)
        console.log('Deck created', created)
        onDone && onDone(`Deck ${created.name} criado. Voce ja pode analisar e otimizar.`, created)
      } else {
        const updated = await updateDeck(currentDeck.id, payload)
        setCurrentDeck(updated)
        await refreshLegality(updated.id)
        console.log('Deck updated', updated)
        setMessage(`Deck ${updated.name} salvo.`)
      }
    } catch (e) {
      console.error('save error', e)
      setError(e.message || 'Falha ao salvar deck.')
    }
  }

  async function handleAnalyze() {
    if (!canAnalyze) return
    try {
      setError(null)
      setLoadingAnalysis(true)
      const deckAnalysis = await getDeckAnalysis(currentDeck.id)
      await refreshLegality(currentDeck.id)
      console.log('analysis', deckAnalysis)
      setAnalysis(deckAnalysis)
      setActivePanel('analysis')
      setMessage('Analise atualizada.')
    } catch (e) {
      console.error('analysis error', e)
      setError(e.message || 'Falha ao buscar analise.')
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
      const deckComparison = await getSimilarDeckComparison(currentDeck.id, params)
      const deckPackages = await getDeckPackages(currentDeck.id)
      console.log('recommendations', recommendations)
      console.info('event=recommendation.request.completed', { deckId: currentDeck.id, count: Array.isArray(recommendations) ? recommendations.length : 0 })
      if (!profile || Number(profile.sampleSize || 0) < 3) {
        console.info('event=recommendation.fallback.rendered', { deckId: currentDeck.id })
      }
      setRec(recommendations)
      setMetaProfile(profile)
      setComparison(deckComparison)
      setPackages(deckPackages)
      setActivePanel('recommendations')
      setMessage(`${Array.isArray(recommendations) ? recommendations.length : 0} recomendacoes estrategicas geradas.`)
    } catch (e) {
      console.error('recommendations error', e)
      console.info('event=recommendation.request.failed', { deckId: currentDeck.id })
      setRecommendationError(e.message || 'Falha ao gerar recomendacoes.')
      setError(e.message || 'Falha ao gerar recomendacoes.')
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
        recommendationId: recommendationKey(item),
        source: item.source,
        confidence: item.confidence,
        problem: item.problem,
        risk: item.risk,
        impactSummary: impactSummary(item),
      })
      setCurrentDeck(updatedDeck)
      setAppliedSwapKeys((previous) => new Set(previous).add(key))
      const deckAnalysis = await getDeckAnalysis(currentDeck.id)
      await refreshLegality(currentDeck.id)
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

  async function handleUndoRecommendation(item) {
    if (!canAnalyze) return
    const key = recommendationKey(item)
    try {
      setError(null)
      setRecommendationError(null)
      setApplyingSwapKey(key)
      const updatedDeck = await undoRecommendationSwap(currentDeck.id, key)
      setCurrentDeck(updatedDeck)
      setAppliedSwapKeys((previous) => {
        const next = new Set(previous)
        next.delete(key)
        return next
      })
      const deckAnalysis = await getDeckAnalysis(currentDeck.id)
      await refreshLegality(currentDeck.id)
      setAnalysis(deckAnalysis)
      setMessage(`Troca desfeita: ${item.remove} voltou ao deck.`)
    } catch (e) {
      console.error('undo recommendation swap error', e)
      setRecommendationError(e.message || 'Nao foi possivel desfazer a troca.')
      setError(e.message || 'Nao foi possivel desfazer a troca.')
    } finally {
      setApplyingSwapKey(null)
    }
  }

  async function handleAddPackage(packageId) {
    if (!canAnalyze || !packageId) return
    try {
      setError(null)
      const updatedDeck = await addPackageToMaybeboard(currentDeck.id, packageId)
      setCurrentDeck(updatedDeck)
      setMessage('Pacote adicionado ao maybeboard.')
    } catch (e) {
      console.error('add package error', e)
      setError(e.message || 'Nao foi possivel adicionar o pacote.')
    }
  }

  return (
    <section>
      <div className="zone zone-command page-heading deck-editor-heading">
        <div>
          <p className="eyebrow">Command Zone</p>
          <h1>{mode === 'create' ? 'Criar Deck' : currentDeck?.name || 'Editar Deck'}</h1>
          <p className="page-description">
            {mode === 'create'
              ? 'Monte a lista primeiro. Depois de salvar, o deck abre direto para legalidade, analise e recomendacoes.'
              : 'Edite a lista, valide a legalidade, analise a estrutura e gere recomendacoes explicaveis.'}
          </p>
        </div>
        <Button variant="secondary" onClick={() => onDone && onDone()}>Voltar aos Decks</Button>
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

      {mode === 'edit' && (
        <div className="card zone zone-command legality-card">
          <DeckLegalityPanel legality={legality} loading={loadingLegality} error={legalityError} />
        </div>
      )}

      {activePanel === 'editor' && (
        <div className="card zone zone-library">
          <DeckForm initial={initial} onCancel={() => onDone && onDone()} onSave={handleSave} />
        </div>
      )}

      {activePanel === 'analysis' && (
        <div className="card zone zone-battlefield">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Metricas do Campo de Batalha</p>
              <h2>Analise</h2>
              <p>Metricas estruturais que orientam decisoes do deck.</p>
            </div>
            <Button onClick={handleAnalyze} disabled={!canAnalyze || loadingAnalysis}>
              <img className="btn-icon" src={analyzeIcon} alt="" aria-hidden="true" />
              {loadingAnalysis ? 'Analisando...' : 'Atualizar analise'}
            </Button>
          </div>
          {analysis ? <DeckAnalysis analysis={analysis} /> : <div className="empty-inline">Execute a analise pelos botoes de acao primeiro.</div>}
        </div>
      )}

      {activePanel === 'recommendations' && (
        <div className="card zone zone-sideboard">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Sideboard / Caminho de Upgrade</p>
              <h2>Recomendacoes</h2>
              <p>Trocas cientes de bracket explicam o problema, por que a adicao ajuda e por que o corte e aceitavel.</p>
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
            onUndoRecommendation={handleUndoRecommendation}
            applyingKey={applyingSwapKey}
            appliedKeys={visibleAppliedSwapKeys}
            comparison={comparison}
            packages={packages}
            history={currentDeck?.history || []}
            onAddPackage={handleAddPackage}
          />
        </div>
      )}

      <div className="card action-card">
        <div>
          <h3>Proximo passo</h3>
          <p>{canAnalyze ? 'Este deck salvo pode ser analisado e otimizado.' : 'Salve um deck valido com ate 99 cartas antes da analise.'}</p>
        </div>
        <div className="actions-row">
          <Button onClick={handleAnalyze} disabled={!canAnalyze || loadingAnalysis}>
            <img className="btn-icon" src={analyzeIcon} alt="" aria-hidden="true" />
            {loadingAnalysis ? 'Analisando...' : 'Analisar Deck'}
          </Button>
          <Button variant="secondary" onClick={() => setActivePanel('recommendations')} disabled={!canAnalyze}>
            <img className="btn-icon" src={recommendIcon} alt="" aria-hidden="true" />
            Abrir recomendacoes
          </Button>
        </div>
      </div>
    </section>
  )
}

function recommendationKey(item) {
  return item?.id || `${item?.add || ''}|||${item?.remove || ''}`.toLowerCase()
}

function impactSummary(item) {
  const impact = item?.impact
  if (!impact) return null
  return `CMC ${Number(impact.averageCmcBefore || 0).toFixed(2)} -> ${Number(impact.averageCmcAfter || 0).toFixed(2)}; ramp ${impact.rampBefore ?? '-'} -> ${impact.rampAfter ?? '-'}; compra ${impact.drawBefore ?? '-'} -> ${impact.drawAfter ?? '-'}; interacao ${impact.removalBefore ?? '-'} -> ${impact.removalAfter ?? '-'}.`
    + ` Game Changers ${impact.gameChangersBefore ?? '-'} -> ${impact.gameChangersAfter ?? '-'}; pressao bracket ${impact.bracketPressureBefore ?? '-'} -> ${impact.bracketPressureAfter ?? '-'}.`
}
