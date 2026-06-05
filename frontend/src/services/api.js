import { clearAuthToken, getAuthToken } from './auth'

const API_ORIGIN = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_URL || 'http://localhost:8080'
const BASE_URL = normalizeApiOrigin(API_ORIGIN)
const REQUEST_TIMEOUT_MS = 25000
const API_STARTUP_RETRY_DELAYS_MS = [1500, 3000, 5000]
const CARD_COLLECTION_BATCH_SIZE = 75

export class ApiStartingError extends Error {
  constructor(message = 'A API está iniciando. Tente novamente em alguns instantes.') {
    super(message)
    this.name = 'ApiStartingError'
    this.code = 'API_STARTING'
  }
}

function normalizeApiOrigin(origin) {
  try {
    const url = new URL(origin)
    if (!['http:', 'https:'].includes(url.protocol)) {
      throw new Error('Unsupported API URL protocol')
    }
    return url.origin.replace(/\/$/, '')
  } catch {
    throw new Error('Invalid VITE_API_URL')
  }
}

async function request(path, options = {}) {
  const retryDelays = options.retryOnStartup === false ? [] : API_STARTUP_RETRY_DELAYS_MS
  const requestOptions = { ...options }
  delete requestOptions.retryOnStartup
  let lastError = null

  for (let attempt = 0; attempt <= retryDelays.length; attempt += 1) {
    try {
      return await requestOnce(path, requestOptions)
    } catch (error) {
      lastError = error
      if (!isStartupTransientError(error) || attempt === retryDelays.length) {
        throw error
      }
      console.info('event=api.startup.waiting', { baseUrl: BASE_URL, attempt: attempt + 1 })
      await wait(retryDelays[attempt])
    }
  }

  throw lastError
}

async function requestOnce(path, options = {}) {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)
  const token = getAuthToken()

  try {
    const res = await fetch(`${BASE_URL}${path}`, {
      credentials: 'omit',
      ...options,
      headers: {
        Accept: 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        'X-Request-Id': createRequestId(),
        'X-Requested-With': 'XMLHttpRequest',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
      signal: controller.signal,
    })

    if (!res.ok) {
      const message = await readErrorMessage(res)
      if (res.status === 401) {
        console.info('event=api.auth.unauthorized action=clear_session')
        clearAuthToken()
      }
      throw new Error(res.status === 401 ? 'Login com Google é obrigatório.' : message || 'Falha na requisição')
    }

    if (res.status === 204) return null
    return await res.json()
  } catch (error) {
    if (isStartupTransientError(error)) {
      throw new ApiStartingError()
    }
    throw error
  } finally {
    window.clearTimeout(timeout)
  }
}

function isStartupTransientError(error) {
  return error?.name === 'AbortError'
    || error?.code === 'API_STARTING'
    || error instanceof TypeError
}

function wait(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

function createRequestId() {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID()
  }
  return `web-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function logApiError(event, error) {
  console.error(event, { name: error?.name || 'Error', code: error?.code || null })
}

async function readErrorMessage(res) {
  const contentType = res.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    const data = await res.json().catch(() => null)
    return data?.message || data?.error || res.statusText
  }

  const text = await res.text().catch(() => '')
  return text.slice(0, 200) || res.statusText
}

export async function fetchDecks({ throwOnError = false } = {}) {
  try {
    return await request('/decks')
  } catch (e) {
    logApiError('fetchDecks error', e)
    if (throwOnError) {
      throw e
    }
    return []
  }
}

export async function fetchPublicDecks({ page = 0, size = 10, commander = '', throwOnError = false } = {}) {
  try {
    const query = new URLSearchParams()
    query.set('page', String(page))
    query.set('size', String(size))
    if (commander) query.set('commander', commander)
    return await request(`/public/decks?${query.toString()}`, { retryOnStartup: false })
  } catch (e) {
    logApiError('fetchPublicDecks error', e)
    if (throwOnError) {
      throw e
    }
    return []
  }
}

export async function getPublicDeck(id) {
  try {
    if (id === undefined || id === null || isNaN(Number(id))) {
      throw new Error('Invalid deck id')
    }
    return await request(`/public/decks/${id}`)
  } catch (e) {
    logApiError('getPublicDeck error', e)
    throw e
  }
}

export async function copyPublicDeck(id) {
  try {
    if (id === undefined || id === null || isNaN(Number(id))) {
      throw new Error('Invalid deck id')
    }
    return await request(`/public/decks/${id}/copy`, { method: 'POST' })
  } catch (e) {
    logApiError('copyPublicDeck error', e)
    throw e
  }
}

export async function likePublicDeck(id) {
  try {
    if (id === undefined || id === null || isNaN(Number(id))) {
      throw new Error('Invalid deck id')
    }
    return await request(`/public/decks/${id}/like`, { method: 'POST' })
  } catch (e) {
    logApiError('likePublicDeck error', e)
    throw e
  }
}

export async function unlikePublicDeck(id) {
  try {
    if (id === undefined || id === null || isNaN(Number(id))) {
      throw new Error('Invalid deck id')
    }
    await request(`/public/decks/${id}/like`, { method: 'DELETE' })
  } catch (e) {
    logApiError('unlikePublicDeck error', e)
    throw e
  }
}

export async function fetchTopPublicDecks({ period = 'WEEKLY', size = 24, throwOnError = false } = {}) {
  try {
    const query = new URLSearchParams()
    query.set('period', period)
    query.set('size', String(size))
    return await request(`/public/decks/top?${query.toString()}`, { retryOnStartup: false })
  } catch (e) {
    logApiError('fetchTopPublicDecks error', e)
    if (throwOnError) {
      throw e
    }
    return []
  }
}

export async function getAppInfo() {
  return await request('/app/info', { retryOnStartup: false })
}

export async function searchCards(name) {
  try {
    return await request(`/cards?name=${encodeURIComponent(name)}`)
  } catch (e) {
    logApiError('searchCards error', e)
    return []
  }
}

export async function createDeck(data) {
  try {
    return await request('/decks', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  } catch (e) {
    logApiError('createDeck error', e)
    throw e
  }
}

export async function updateDeck(id, data) {
  try {
    if (id === undefined || id === null || isNaN(Number(id))) {
      throw new Error('Invalid deck id')
    }
    return await request(`/decks/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    })
  } catch (e) {
    logApiError('updateDeck error', e)
    throw e
  }
}

export async function deleteDeck(id) {
  try {
    if (id === undefined || id === null || isNaN(Number(id))) {
      throw new Error('Invalid deck id')
    }
    await request(`/decks/${id}`, { method: 'DELETE' })
  } catch (e) {
    logApiError('deleteDeck error', e)
    throw e
  }
}

export async function getDeckAnalysis(id) {
  try {
    if (id === undefined || id === null || isNaN(Number(id))) {
      throw new Error('Invalid deck id')
    }
    return await request(`/decks/${id}/analysis`)
  } catch (e) {
    logApiError('getDeckAnalysis error', e)
    throw e
  }
}

export async function getDeckLegality(id) {
  try {
    if (id === undefined || id === null || isNaN(Number(id))) {
      throw new Error('Id do deck inválido')
    }
    return await request(`/decks/${id}/legality`)
  } catch (e) {
    logApiError('getDeckLegality error', e)
    throw e
  }
}

export async function getRecommendations(id, params) {
  return getStrategicRecommendations(id, params)
}

export async function fetchCardsByNames(names = []) {
  const uniqueNames = [...new Set(names.map((name) => String(name || '').trim()).filter(Boolean))]
  if (uniqueNames.length === 0) return []
  try {
    const cards = []
    for (let start = 0; start < uniqueNames.length; start += CARD_COLLECTION_BATCH_SIZE) {
      const batch = uniqueNames.slice(start, start + CARD_COLLECTION_BATCH_SIZE)
      const resolved = await request('/cards/collection', {
        method: 'POST',
        body: JSON.stringify({ names: batch }),
      })
      cards.push(...resolved)
    }
    return cards
  } catch (e) {
    logApiError('fetchCardsByNames error', e)
    return []
  }
}

export async function getStrategicRecommendations(id, params) {
  try {
    if (id === undefined || id === null || isNaN(Number(id))) {
      throw new Error('Invalid deck id')
    }
    return await request(`/decks/${id}/recommendations/strategic`, {
      method: 'POST',
      body: JSON.stringify(params || {}),
    })
  } catch (e) {
    logApiError('getStrategicRecommendations error', e)
    throw e
  }
}

export async function applyRecommendationSwap(deckId, payload) {
  try {
    if (deckId === undefined || deckId === null || isNaN(Number(deckId))) {
      throw new Error('Invalid deck id')
    }
    return await request(`/decks/${deckId}/recommendations/apply-swap`, {
      method: 'POST',
      body: JSON.stringify(payload || {}),
    })
  } catch (e) {
    logApiError('applyRecommendationSwap error', e)
    throw e
  }
}

export async function undoRecommendationSwap(deckId, recommendationId) {
  try {
    if (deckId === undefined || deckId === null || isNaN(Number(deckId))) {
      throw new Error('Invalid deck id')
    }
    return await request(`/decks/${deckId}/recommendations/undo-swap`, {
      method: 'POST',
      body: JSON.stringify({ recommendationId }),
    })
  } catch (e) {
    logApiError('undoRecommendationSwap error', e)
    throw e
  }
}

export async function getSimilarDeckComparison(deckId, params) {
  try {
    if (deckId === undefined || deckId === null || isNaN(Number(deckId))) {
      throw new Error('Invalid deck id')
    }
    return await request(`/decks/${deckId}/comparison`, {
      method: 'POST',
      body: JSON.stringify(params || {}),
    })
  } catch (e) {
    logApiError('getSimilarDeckComparison error', e)
    return null
  }
}

export async function getMetaSources() {
  try {
    const json = await request('/meta/sources')
    return json.sources || []
  } catch (e) {
    logApiError('getMetaSources error', e)
    return []
  }
}

export async function getCommanderMeta(commander, { bracket = 'casual', sourceMode = 'auto' } = {}) {
  if (!commander) return null
  try {
    const query = new URLSearchParams({ bracket, sourceMode })
    return await request(`/meta/commanders/${encodeURIComponent(commander)}?${query}`)
  } catch (e) {
    logApiError('getCommanderMeta error', e)
    return null
  }
}

export async function syncMeta(adminKey = '') {
  try {
    return await request('/meta/sync', {
      method: 'POST',
      retryOnStartup: false,
      headers: adminKey ? { 'X-Admin-Key': adminKey } : {},
    })
  } catch (e) {
    logApiError('syncMeta error', e)
    throw e
  }
}

export async function submitRecommendationFeedback(auditId, status) {
  try {
    return await request(`/recommendation-audits/${auditId}/feedback`, {
      method: 'POST',
      retryOnStartup: false,
      body: JSON.stringify({ status }),
    })
  } catch (e) {
    logApiError('submitRecommendationFeedback error', e)
    throw e
  }
}

export async function getRecommendationBenchmarkSummary(adminKey = '') {
  try {
    return await request('/meta/recommendation-benchmark/summary', {
      retryOnStartup: false,
      headers: adminKey ? { 'X-Admin-Key': adminKey } : {},
    })
  } catch (e) {
    logApiError('getRecommendationBenchmarkSummary error', e)
    throw e
  }
}

export async function importDeck(data) {
  try {
    return await request('/decks/import', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  } catch (e) {
    logApiError('importDeck error', e)
    throw e
  }
}

export async function exportUserData() {
  return await request('/users/me/export', { retryOnStartup: false })
}

export async function deleteAccountData() {
  await request('/users/me', { method: 'DELETE' })
}

export async function checkSecurityStatus({ includeDetails = false, scanExternalDependencies = false } = {}) {
  console.info('event=api.security_status.request', {
    includeDetails: Boolean(includeDetails),
    scanExternalDependencies: Boolean(scanExternalDependencies),
  })
  return await request('/security/status/check', {
    method: 'POST',
    retryOnStartup: false,
    body: JSON.stringify({
      includeDetails: Boolean(includeDetails),
      scanExternalDependencies: Boolean(scanExternalDependencies),
    }),
  })
}

export default {
  fetchDecks,
  fetchPublicDecks,
  getPublicDeck,
  copyPublicDeck,
  likePublicDeck,
  unlikePublicDeck,
  fetchTopPublicDecks,
  fetchCardsByNames,
  searchCards,
  createDeck,
  updateDeck,
  deleteDeck,
  getDeckAnalysis,
  getDeckLegality,
  getRecommendations,
  getStrategicRecommendations,
  applyRecommendationSwap,
  undoRecommendationSwap,
  getSimilarDeckComparison,
  getMetaSources,
  getCommanderMeta,
  syncMeta,
  submitRecommendationFeedback,
  getRecommendationBenchmarkSummary,
  getAppInfo,
  exportUserData,
  deleteAccountData,
  checkSecurityStatus,
}
