import { getAuthToken } from './auth'

const API_ORIGIN = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const BASE_URL = normalizeApiOrigin(API_ORIGIN)
const REQUEST_TIMEOUT_MS = 25000
const API_STARTUP_RETRY_DELAYS_MS = [1500, 3000, 5000]
const CARD_COLLECTION_BATCH_SIZE = 75

export class ApiStartingError extends Error {
  constructor(message = 'A API esta iniciando. Tente novamente em alguns instantes.') {
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
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
      signal: controller.signal,
    })

    if (!res.ok) {
      const message = await readErrorMessage(res)
      throw new Error(res.status === 401 ? 'Login com Google é obrigatorio.' : message || 'Falha na requisição')
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
    console.error('fetchDecks error', e)
    if (throwOnError) {
      throw e
    }
    return []
  }
}

export async function searchCards(name) {
  try {
    return await request(`/cards?name=${encodeURIComponent(name)}`)
  } catch (e) {
    console.error('searchCards error', e)
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
    console.error('createDeck error', e)
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
    console.error('updateDeck error', e)
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
    console.error('deleteDeck error', e)
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
    console.error('getDeckAnalysis error', e)
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
    console.error('getDeckLegality error', e)
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
    console.error('fetchCardsByNames error', e)
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
    console.error('getStrategicRecommendations error', e)
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
    console.error('applyRecommendationSwap error', e)
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
    console.error('undoRecommendationSwap error', e)
    throw e
  }
}

export async function getDeckPackages(deckId) {
  try {
    if (deckId === undefined || deckId === null || isNaN(Number(deckId))) {
      throw new Error('Invalid deck id')
    }
    return await request(`/decks/${deckId}/packages`)
  } catch (e) {
    console.error('getDeckPackages error', e)
    return []
  }
}

export async function addPackageToMaybeboard(deckId, packageId) {
  try {
    if (deckId === undefined || deckId === null || isNaN(Number(deckId))) {
      throw new Error('Invalid deck id')
    }
    return await request(`/decks/${deckId}/packages/${encodeURIComponent(packageId)}/maybeboard`, {
      method: 'POST',
      body: JSON.stringify({}),
    })
  } catch (e) {
    console.error('addPackageToMaybeboard error', e)
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
    console.error('getSimilarDeckComparison error', e)
    return null
  }
}

export async function getMetaSources() {
  try {
    const json = await request('/meta/sources')
    return json.sources || []
  } catch (e) {
    console.error('getMetaSources error', e)
    return []
  }
}

export async function getCommanderMeta(commander, { bracket = 'casual', sourceMode = 'auto' } = {}) {
  if (!commander) return null
  try {
    const query = new URLSearchParams({ bracket, sourceMode })
    return await request(`/meta/commanders/${encodeURIComponent(commander)}?${query}`)
  } catch (e) {
    console.error('getCommanderMeta error', e)
    return null
  }
}

export async function importDeck(data) {
  try {
    return await request('/decks/import', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  } catch (e) {
    console.error('importDeck error', e)
    throw e
  }
}

export default {
  fetchDecks,
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
  getDeckPackages,
  addPackageToMaybeboard,
  getSimilarDeckComparison,
  getMetaSources,
  getCommanderMeta,
}
