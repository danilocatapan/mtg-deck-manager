import { getAuthToken } from './auth'

const API_ORIGIN = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const BASE_URL = normalizeApiOrigin(API_ORIGIN)
const REQUEST_TIMEOUT_MS = 12000

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
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
      signal: controller.signal,
    })

    if (!res.ok) {
      const message = await readErrorMessage(res)
      throw new Error(res.status === 401 ? 'Login with Google is required.' : message || 'Request failed')
    }

    if (res.status === 204) return null
    return await res.json()
  } finally {
    window.clearTimeout(timeout)
  }
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

export async function fetchDecks() {
  try {
    return await request('/decks')
  } catch (e) {
    console.error('fetchDecks error', e)
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

export async function getRecommendations(id, params) {
  return getStrategicRecommendations(id, params)
}

export async function fetchCardsByNames(names = []) {
  const uniqueNames = [...new Set(names.map((name) => String(name || '').trim()).filter(Boolean))]
  if (uniqueNames.length === 0) return []
  try {
    return await request('/cards/collection', {
      method: 'POST',
      body: JSON.stringify({ names: uniqueNames }),
    })
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
  getRecommendations,
  getStrategicRecommendations,
  getMetaSources,
  getCommanderMeta,
}
