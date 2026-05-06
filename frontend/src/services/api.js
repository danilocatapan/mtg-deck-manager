const API_ORIGIN = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const BASE_URL = API_ORIGIN.replace(/\/$/, '')

export async function fetchDecks() {
  try {
    console.log('fetchDecks: calling', `${BASE_URL}/decks`)
    const res = await fetch(`${BASE_URL}/decks`)
    if (!res.ok) throw new Error('Failed to fetch decks')
    const json = await res.json()
    console.log('fetchDecks result', json)
    return json
  } catch (e) {
    console.error('fetchDecks error', e)
    return []
  }
}

export async function searchCards(name) {
  try {
    console.log('searchCards:', name)
    const res = await fetch(`${BASE_URL}/cards?name=${encodeURIComponent(name)}`)
    if (!res.ok) throw new Error('Failed to search cards')
    const json = await res.json()
    console.log('searchCards result', json)
    return json
  } catch (e) {
    console.error('searchCards error', e)
    return []
  }
}

export async function createDeck(data) {
  try {
    console.log('createDeck', data)
    const res = await fetch(`${BASE_URL}/decks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    })
    if (!res.ok) throw new Error('Failed to create deck')
    const json = await res.json()
    console.log('createDeck result', json)
    return json
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
    console.log('updateDeck', id, data)
    const res = await fetch(`${BASE_URL}/decks/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    })
    if (!res.ok) throw new Error('Failed to update deck')
    const json = await res.json()
    console.log('updateDeck result', json)
    return json
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
    console.log('deleteDeck', id)
    await fetch(`${BASE_URL}/decks/${id}`, { method: 'DELETE' })
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
    console.log('getDeckAnalysis', id)
    const res = await fetch(`${BASE_URL}/decks/${id}/analysis`)
    if (!res.ok) throw new Error('Failed to get analysis')
    const json = await res.json()
    console.log('getDeckAnalysis result', json)
    return json
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
    console.log('fetchCardsByNames:', uniqueNames.length)
    const res = await fetch(`${BASE_URL}/cards/collection`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ names: uniqueNames }),
    })
    if (!res.ok) throw new Error('Failed to fetch card collection')
    return await res.json()
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
    console.log('getStrategicRecommendations', id, params)
    const res = await fetch(`${BASE_URL}/decks/${id}/recommendations/strategic`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params || {}),
    })
    if (!res.ok) {
      const text = await res.text()
      throw new Error(text || 'Failed to get strategic recommendations')
    }
    const json = await res.json()
    console.log('getStrategicRecommendations result', json)
    return json
  } catch (e) {
    console.error('getStrategicRecommendations error', e)
    throw e
  }
}

export async function getMetaSources() {
  try {
    const res = await fetch(`${BASE_URL}/meta/sources`)
    if (!res.ok) throw new Error('Failed to get meta sources')
    const json = await res.json()
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
    const res = await fetch(`${BASE_URL}/meta/commanders/${encodeURIComponent(commander)}?${query}`)
    if (!res.ok) throw new Error('Failed to get commander meta')
    return await res.json()
  } catch (e) {
    console.error('getCommanderMeta error', e)
    return null
  }
}

export async function importDeck(data) {
  try {
    console.log('importDeck', data)
    const res = await fetch(`${BASE_URL}/decks/import`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    })
    if (!res.ok) {
      const text = await res.text()
      throw new Error(text || 'Failed to import deck')
    }
    const json = await res.json()
    console.log('importDeck result', json)
    return json
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
