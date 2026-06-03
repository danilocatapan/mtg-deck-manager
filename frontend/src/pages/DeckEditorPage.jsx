import { useCallback, useEffect, useMemo, useState } from 'react'
import DeckForm from '../components/DeckForm'
import DeckAnalysis from '../components/DeckAnalysis'
import DeckLegalityPanel from '../components/DeckLegalityPanel'
import RecommendationPanel from '../components/recommendations/RecommendationPanel'
import RecommendationSettings from '../components/recommendations/RecommendationSettings'
import FloatingActionBar from '../components/FloatingActionBar'
import {
  applyRecommendationSwap,
  createDeck,
  getCommanderMeta,
  getDeckAnalysis,
  getDeckLegality,
  getMetaSources,
  getRecommendations,
  getSimilarDeckComparison,
  undoRecommendationSwap,
  updateDeck,
} from '../services/api'
import Button from '../components/ui/Button'
import ModalDialog from '../components/ui/ModalDialog'
import analyzeIcon from '../assets/icons/analyze.png'
import recommendIcon from '../assets/icons/recommend.png'

export default function DeckEditorPage({ mode = 'create', deck = null, initialMessage = null, initialPanel = 'edit', onPanelChange, onDone }) {
  const [analysis, setAnalysis] = useState(null)
  const [legality, setLegality] = useState(null)
  const [rec, setRec] = useState(null)
  const [metaProfile, setMetaProfile] = useState(null)
  const [metaSources, setMetaSources] = useState([])
  const [comparison, setComparison] = useState(null)
  const [recommendationParams, setRecommendationParams] = useState({ bracket: 'casual' })
  const [loadingAnalysis, setLoadingAnalysis] = useState(false)
  const [loadingLegality, setLoadingLegality] = useState(false)
  const [loadingRec, setLoadingRec] = useState(false)
  const [legalityError, setLegalityError] = useState(null)
  const [recommendationError, setRecommendationError] = useState(null)
  const [message, setMessage] = useState(initialMessage)
  const [error, setError] = useState(null)
  const [activePanel, setActivePanel] = useState(panelToActiveKey(initialPanel))
  const [currentDeck, setCurrentDeck] = useState(deck)
  const [applyingSwapKey, setApplyingSwapKey] = useState(null)
  const [appliedSwapKeys, setAppliedSwapKeys] = useState(() => new Set())
  const [pendingRecommendation, setPendingRecommendation] = useState(null)

  const initial = mode === 'edit' ? currentDeck : null
  const savedCardCount = useMemo(() => currentDeck?.cards
    .reduce((sum, card) => sum + Number(card.quantity || 0), 0) ?? 0, [currentDeck])
  const canAnalyze = mode === 'edit' && currentDeck?.id && savedCardCount > 0 && savedCardCount <= 99

  useEffect(() => {
    const nextPanel = panelToActiveKey(initialPanel)
    if (nextPanel === activePanel) return
    queueMicrotask(() => setActivePanel(nextPanel))
  }, [activePanel, initialPanel])

  const refreshLegality = useCallback(async function refreshDeckLegality(deckId = currentDeck?.id) {
    if (!deckId) return
    try {
      setLoadingLegality(true)
      setLegalityError(null)
      const deckLegality = await getDeckLegality(deckId)
      setLegality(deckLegality)
    } catch (e) {
      console.error('legality error')
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
    { key: 'analysis', label: 'Análise', state: activePanel === 'analysis' ? 'active' : analysis ? 'complete' : 'locked' },
    { key: 'recommendations', label: 'Recomendações', state: activePanel === 'recommendations' ? 'active' : rec ? 'complete' : 'locked' },
  ]

  async function handleSave(payload) {
    try {
      setError(null)
      if (mode === 'create') {
        const created = await createDeck(payload)
        onDone && onDone(`Deck ${created.name} criado. Você já pode analisar e otimizar.`, created)
      } else {
        const updated = await updateDeck(currentDeck.id, payload)
        setCurrentDeck(updated)
        await refreshLegality(updated.id)
        setMessage(`Deck ${updated.name} salvo.`)
      }
    } catch (e) {
      console.error('save error')
      setError(e.message || 'Falha ao salvar deck.')
      throw e
    }
  }

  async function handleAnalyze() {
    if (!canAnalyze) return
    try {
      setError(null)
      setLoadingAnalysis(true)
      const deckAnalysis = await getDeckAnalysis(currentDeck.id)
      await refreshLegality(currentDeck.id)
      setAnalysis(deckAnalysis)
      changePanel('analysis')
      setMessage('Análise atualizada.')
    } catch (e) {
      console.error('analysis error')
      setError(e.message || 'Falha ao buscar análise.')
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
      const recommendationRun = await getRecommendations(currentDeck.id, params)
      const recommendationItems = Array.isArray(recommendationRun) ? recommendationRun : recommendationRun?.recommendations || []
      const profile = await getCommanderMeta(currentDeck.commander, {
        bracket: params?.bracket || 'casual',
      })
      const deckComparison = await getSimilarDeckComparison(currentDeck.id, params)
      console.info('event=recommendation.request.completed', { deckId: currentDeck.id, count: recommendationItems.length, confidence: recommendationRun?.confidence || null })
      if (!profile || Number(profile.sampleSize || 0) < 3) {
        console.info('event=recommendation.fallback.rendered', { deckId: currentDeck.id })
      }
      setRec(recommendationRun)
      setMetaProfile(profile)
      setComparison(deckComparison)
      changePanel('recommendations')
      setMessage(`${recommendationItems.length} recomendações estratégicas geradas.`)
    } catch (e) {
      console.error('recommendations error')
      console.info('event=recommendation.request.failed', { deckId: currentDeck.id })
      setRecommendationError(e.message || 'Falha ao gerar recomendações.')
      setError(e.message || 'Falha ao gerar recomendações.')
    } finally {
      setLoadingRec(false)
    }
  }

  async function handleApplyRecommendation(item) {
    if (!canAnalyze || !item?.add || !item?.remove) return
    setPendingRecommendation(item)
  }

  async function confirmApplyRecommendation() {
    const item = pendingRecommendation
    if (!canAnalyze || !item?.add || !item?.remove) return
    const key = recommendationKey(item)
    try {
      setError(null)
      setRecommendationError(null)
      setPendingRecommendation(null)
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
      console.info('event=recommendation.swap.applied', { deckId: currentDeck.id })
    } catch (e) {
      console.error('apply recommendation swap error')
      setRecommendationError(e.message || 'Não foi possível aplicar a troca.')
      setError(e.message || 'Não foi possível aplicar a troca.')
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
      console.error('undo recommendation swap error')
      setRecommendationError(e.message || 'Não foi possível desfazer a troca.')
      setError(e.message || 'Não foi possível desfazer a troca.')
    } finally {
      setApplyingSwapKey(null)
    }
  }

  function scrollToTop() {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    window.scrollTo({ top: 0, behavior: reducedMotion ? 'auto' : 'smooth' })
  }

  function changePanel(panel) {
    setActivePanel(panel)
    onPanelChange?.(activeKeyToRoutePanel(panel))
  }

  return (
    <section>
      <div className="zone zone-command page-heading deck-editor-heading">
        <div>
          <p className="eyebrow">Command Zone</p>
          <h1>{mode === 'create' ? 'Criar Deck' : currentDeck?.name || 'Editar Deck'}</h1>
          <p className="page-description">
            {mode === 'create'
              ? 'Monte a lista primeiro. Depois de salvar, o deck abre direto para legalidade, análise e recomendações.'
              : 'Edite a lista, valide a legalidade, analise a estrutura e gere recomendações explicáveis.'}
          </p>
        </div>
        <Button variant="secondary" onClick={() => onDone && onDone()}>Voltar aos Decks</Button>
      </div>

      <nav className="workflow-stepper" aria-label="Fluxo do deck">
        {steps.map((step, index) => (
          <button
            key={step.key}
            type="button"
            className="step"
            data-state={step.state}
            aria-current={step.state === 'active' ? 'step' : undefined}
            onClick={() => changePanel(step.key)}
            disabled={step.state === 'locked'}
          >
            <strong>{index + 1}</strong>
            <span>{step.label}</span>
          </button>
        ))}
      </nav>

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
              <p className="eyebrow">Métricas do Campo de Batalha</p>
              <h2>Análise</h2>
              <p>Métricas estruturais que orientam decisões do deck.</p>
            </div>
            <Button onClick={handleAnalyze} disabled={!canAnalyze || loadingAnalysis}>
              <img className="btn-icon" src={analyzeIcon} alt="" aria-hidden="true" />
              {loadingAnalysis ? 'Analisando...' : 'Atualizar análise'}
            </Button>
          </div>
          {analysis ? <DeckAnalysis analysis={analysis} /> : <div className="empty-inline">Execute a análise pelos botões de ação primeiro.</div>}
        </div>
      )}

      {activePanel === 'recommendations' && (
        <div className="card zone zone-planning">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Otimização</p>
              <h2>Recomendações</h2>
              <p>Trocas cientes de bracket explicam o problema, por que a adição ajuda e por que o corte é aceitável.</p>
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
            history={currentDeck?.history || []}
          />
        </div>
      )}

      <div className="card action-card editor-next-step-card">
        <div>
          <h3>Próximo passo</h3>
          <p>{canAnalyze ? 'Este deck salvo pode ser analisado e otimizado.' : 'Salve um deck válido com até 99 cartas antes da análise.'}</p>
        </div>
        <div className="actions-row">
          <Button onClick={handleAnalyze} disabled={!canAnalyze || loadingAnalysis}>
            <img className="btn-icon" src={analyzeIcon} alt="" aria-hidden="true" />
            {loadingAnalysis ? 'Analisando...' : 'Analisar Deck'}
          </Button>
          <Button variant="secondary" onClick={() => changePanel('recommendations')} disabled={!canAnalyze}>
            <img className="btn-icon" src={recommendIcon} alt="" aria-hidden="true" />
            Abrir recomendações
          </Button>
        </div>
      </div>
      {mode === 'edit' && (
        <FloatingActionBar
          label="Acoes do deck"
          actions={[
            { label: 'Analisar', onClick: handleAnalyze, disabled: !canAnalyze || loadingAnalysis, icon: analyzeIcon },
            { label: 'Recomendacoes', onClick: () => changePanel('recommendations'), disabled: !canAnalyze, icon: recommendIcon },
            { label: 'Topo', onClick: scrollToTop },
          ]}
        />
      )}
      <ModalDialog
        open={Boolean(pendingRecommendation)}
        onClose={() => setPendingRecommendation(null)}
        labelledBy="swap-confirm-title"
        describedBy="swap-confirm-description"
        className="confirm-dialog swap-confirm-dialog"
      >
        {pendingRecommendation && (
          <>
            <div>
              <p className="eyebrow">Recomendação</p>
              <h2 id="swap-confirm-title">Aplicar troca?</h2>
            </div>
            <p id="swap-confirm-description">
              Vamos adicionar <strong>{pendingRecommendation.add}</strong> e remover <strong>{pendingRecommendation.remove}</strong>.
              A troca ficará no histórico do deck e poderá ser desfeita depois.
            </p>
            <div className="swap-confirm-route" aria-hidden="true">
              <span className="swap-card remove"><small>Sai</small><strong>{pendingRecommendation.remove}</strong></span>
              <span className="swap-arrow">→</span>
              <span className="swap-card add"><small>Entra</small><strong>{pendingRecommendation.add}</strong></span>
            </div>
            <div className="confirm-dialog-actions">
              <Button variant="secondary" onClick={() => setPendingRecommendation(null)} data-autofocus>Cancelar</Button>
              <Button onClick={confirmApplyRecommendation} loading={Boolean(applyingSwapKey)} loadingLabel="Aplicando troca...">Aplicar troca</Button>
            </div>
          </>
        )}
      </ModalDialog>
    </section>
  )
}

function recommendationKey(item) {
  return item?.id || `${item?.add || ''}|||${item?.remove || ''}`.toLowerCase()
}

function panelToActiveKey(panel) {
  if (panel === 'analysis' || panel === 'recommendations') return panel
  return 'editor'
}

function activeKeyToRoutePanel(panel) {
  if (panel === 'analysis' || panel === 'recommendations') return panel
  return 'edit'
}

function impactSummary(item) {
  const impact = item?.impact
  if (!impact) return null
  return `CMC ${Number(impact.averageCmcBefore || 0).toFixed(2)} -> ${Number(impact.averageCmcAfter || 0).toFixed(2)}; ramp ${impact.rampBefore ?? '-'} -> ${impact.rampAfter ?? '-'}; compra ${impact.drawBefore ?? '-'} -> ${impact.drawAfter ?? '-'}; interação ${impact.removalBefore ?? '-'} -> ${impact.removalAfter ?? '-'}.`
    + ` Game Changers ${impact.gameChangersBefore ?? '-'} -> ${impact.gameChangersAfter ?? '-'}; pressão bracket ${impact.bracketPressureBefore ?? '-'} -> ${impact.bracketPressureAfter ?? '-'}.`
}
