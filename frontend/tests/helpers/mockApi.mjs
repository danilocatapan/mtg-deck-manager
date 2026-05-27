export const sampleCards = [
  { name: 'Sol Ring', quantity: 1, typeLine: 'Artifact', manaValue: 1, imageUrl: '' },
  { name: 'Arcane Signet', quantity: 1, typeLine: 'Artifact', manaValue: 2, imageUrl: '' },
  { name: 'Beast Whisperer', quantity: 1, typeLine: 'Creature', manaValue: 4, imageUrl: '' },
  { name: 'Colossal Dreadmaw', quantity: 1, typeLine: 'Creature', manaValue: 6, imageUrl: '' },
  { name: 'Mountain', quantity: 12, typeLine: 'Basic Land', manaValue: 0, imageUrl: '' },
  { name: 'Forest', quantity: 12, typeLine: 'Basic Land', manaValue: 0, imageUrl: '' },
]

export const userDeck = {
  id: 1,
  name: 'Gruul Revels',
  commander: 'Xenagos, God of Revels',
  visibility: 'private',
  colorIdentity: ['R', 'G'],
  mainDeckSize: 28,
  cardCount: 28,
  cards: sampleCards,
  history: [],
}

export const publicDeck = {
  ...userDeck,
  id: 101,
  name: 'Public Gruul Revels',
  visibility: 'public',
  ownedByCurrentUser: false,
  likeCount: 3,
  likedByCurrentUser: false,
  author: 'Mesa Teste',
}

export const analysis = {
  deckId: 1,
  averageManaValue: 2.75,
  manaCurve: { 0: 24, 1: 1, 2: 1, 4: 1, 6: 1 },
  manaCurveCards: {
    1: ['Sol Ring'],
    2: ['Arcane Signet'],
    4: ['Beast Whisperer'],
    6: ['Colossal Dreadmaw'],
  },
  roleCounts: {
    ramp: 2,
    draw: 1,
    removal: 1,
    protection: 0,
    boardWipe: 0,
    winCondition: 1,
    lands: 24,
  },
  roleCards: {
    ramp: ['Sol Ring', 'Arcane Signet'],
    draw: ['Beast Whisperer'],
    winCondition: ['Colossal Dreadmaw'],
    lands: ['Mountain', 'Forest'],
  },
  combos: [],
}

export const recommendation = {
  id: 'rec-beast-whisperer',
  add: 'Beast Whisperer',
  remove: 'Colossal Dreadmaw',
  source: 'meta_profile',
  confidence: 'high',
  bracket: 'casual',
  problem: 'A lista precisa de mais compra para manter pressao depois do comandante.',
  reasoning: 'Beast Whisperer transforma criaturas futuras em cartas e reduz risco de ficar sem gas.',
  risk: 'Risco baixo: a carta removida e apenas corpo grande sem sinergia central.',
  sourceContext: { sampleSize: 12 },
  impact: {
    role: 'draw',
    averageCmcBefore: 2.75,
    averageCmcAfter: 2.58,
    rampBefore: 2,
    rampAfter: 2,
    drawBefore: 1,
    drawAfter: 2,
    removalBefore: 1,
    removalAfter: 1,
    gameChangersBefore: 0,
    gameChangersAfter: 0,
    bracketPressureBefore: 2,
    bracketPressureAfter: 2,
  },
}

export function fakeGoogleToken() {
  const encode = (value) => Buffer.from(JSON.stringify(value), 'utf8').toString('base64url')
  const now = Math.floor(Date.now() / 1000)
  return [
    encode({ alg: 'none', typ: 'JWT' }),
    encode({
      iss: 'accounts.google.com',
      aud: 'test-client',
      sub: 'test-user',
      email: 'tester@example.com',
      name: 'Tester',
      exp: now + 3600,
      iat: now,
    }),
    'signature',
  ].join('.')
}

export async function installAuth(page) {
  await page.addInitScript((token) => {
    const now = Math.floor(Date.now() / 1000)
    window.sessionStorage.setItem('mtg_google_id_token', token)
    window.sessionStorage.setItem('mtg_google_profile', JSON.stringify({
      sub: 'test-user',
      email: 'tester@example.com',
      name: 'Tester',
      picture: '',
      iss: 'accounts.google.com',
      aud: 'test-client',
      exp: now + 3600,
      iat: now,
    }))
  }, fakeGoogleToken())
}

export async function mockApi(page) {
  let currentUserDeck = structuredClone(userDeck)
  let publicDeckState = structuredClone(publicDeck)

  await page.route(/http:\/\/(localhost|127\.0\.0\.1):8080\/.*/, async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const path = url.pathname
    const method = request.method()

    if (path === '/app/info') {
      return json(route, { name: 'MTG Deck Manager API', version: 'test', objective: 'API mockada para UX.' })
    }

    if (path === '/public/decks' && method === 'GET') {
      return json(route, [publicDeckState])
    }

    if (path === '/public/decks/101' && method === 'GET') {
      return json(route, publicDeckState)
    }

    if (path === '/public/decks/101/copy' && method === 'POST') {
      currentUserDeck = { ...publicDeckState, id: 2, visibility: 'private', ownedByCurrentUser: true, name: `${publicDeckState.name} copia` }
      return json(route, currentUserDeck)
    }

    if (path === '/public/decks/101/like' && method === 'POST') {
      publicDeckState = { ...publicDeckState, likedByCurrentUser: true, likeCount: publicDeckState.likeCount + 1 }
      return json(route, publicDeckState)
    }

    if (path === '/public/decks/101/like' && method === 'DELETE') {
      publicDeckState = { ...publicDeckState, likedByCurrentUser: false, likeCount: Math.max(0, publicDeckState.likeCount - 1) }
      return route.fulfill({ status: 204 })
    }

    if (path === '/decks' && method === 'GET') {
      return json(route, currentUserDeck ? [currentUserDeck] : [])
    }

    if (path === '/decks' && method === 'POST') {
      const payload = await request.postDataJSON()
      currentUserDeck = { ...currentUserDeck, ...payload, id: 1, cards: payload.cards || [] }
      return json(route, currentUserDeck)
    }

    if (path === '/decks/1' && method === 'PUT') {
      const payload = await request.postDataJSON()
      currentUserDeck = { ...currentUserDeck, ...payload, cards: payload.cards || currentUserDeck.cards }
      return json(route, currentUserDeck)
    }

    if (path === '/decks/1' && method === 'DELETE') {
      currentUserDeck = null
      return route.fulfill({ status: 204 })
    }

    if (path === '/decks/import' && method === 'POST') {
      const payload = await request.postDataJSON()
      currentUserDeck = {
        ...userDeck,
        id: 1,
        name: payload.name,
        commander: payload.commander,
        visibility: payload.visibility || 'private',
      }
      return json(route, currentUserDeck)
    }

    if (path === '/decks/1/legality') {
      return json(route, { legal: true, status: 'legal', messages: [], colorIdentityOk: true, singletonOk: true, deckSizeOk: true })
    }

    if (path === '/decks/1/analysis') {
      return json(route, analysis)
    }

    if (path === '/decks/1/recommendations/strategic') {
      return json(route, [recommendation])
    }

    if (path === '/decks/1/recommendations/apply-swap') {
      currentUserDeck = {
        ...currentUserDeck,
        history: [{ id: recommendation.id, add: recommendation.add, remove: recommendation.remove, undone: false }],
      }
      return json(route, currentUserDeck)
    }

    if (path === '/decks/1/recommendations/undo-swap') {
      currentUserDeck = {
        ...currentUserDeck,
        history: [{ id: recommendation.id, add: recommendation.add, remove: recommendation.remove, undone: true }],
      }
      return json(route, currentUserDeck)
    }

    if (path === '/decks/1/comparison') {
      return json(route, {
        commander: currentUserDeck?.commander || userDeck.commander,
        sampleSize: 12,
        metrics: [{ key: 'draw', label: 'Compra', status: 'warning', deckValue: 1, similarAverage: 5, message: 'Abaixo da media.' }],
      })
    }

    if (path === '/meta/sources') {
      return json(route, { sources: [{ name: 'top_decks', enabled: true }] })
    }

    if (path.startsWith('/meta/commanders/')) {
      return json(route, { commander: 'Xenagos, God of Revels', sampleSize: 12, bracket: 'casual' })
    }

    if (path === '/cards/collection' && method === 'POST') {
      const payload = await request.postDataJSON()
      return json(route, (payload.names || []).map((name) => ({
        name,
        typeLine: name === 'Forest' || name === 'Mountain' ? 'Basic Land' : 'Creature',
        imageUrl: '',
      })))
    }

    if (path === '/cards' && method === 'GET') {
      const name = url.searchParams.get('name') || 'Sol Ring'
      return json(route, [{ name, typeLine: 'Artifact', imageUrl: '' }])
    }

    return json(route, { message: `Unhandled mock route: ${method} ${path}` }, 404)
  })
}

function json(route, body, status = 200) {
  return route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  })
}
