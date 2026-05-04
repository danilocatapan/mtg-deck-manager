const BASE_URL = 'http://localhost:8080'

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

export default { fetchDecks, searchCards }
