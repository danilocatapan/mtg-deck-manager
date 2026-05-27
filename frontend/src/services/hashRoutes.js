export const HASH_ROUTES = {
  HOME: 'home',
  CONTACT: 'contact',
  RELEASE_NOTES: 'release-notes',
  META_ADMIN: 'meta-admin',
  IMPORT: 'import',
  PUBLIC_DECK: 'public-deck',
  DECK_EDITOR: 'deck-editor',
}

const LEGACY_HASHES = {
  contact: HASH_ROUTES.CONTACT,
  'release-notes': HASH_ROUTES.RELEASE_NOTES,
  'meta-admin': HASH_ROUTES.META_ADMIN,
  import: HASH_ROUTES.IMPORT,
}

export function parseHashRoute(hash = '') {
  const value = String(hash || '').replace(/^#\/?/, '').trim()
  if (!value) return { name: HASH_ROUTES.HOME }
  const [path, queryString = ''] = value.split('?')
  const query = new URLSearchParams(queryString)

  const [routeName, routeParam, routePanel] = path.split('/').filter(Boolean)
  if (routeName === 'public' && routeParam) {
    return {
      name: HASH_ROUTES.PUBLIC_DECK,
      deckId: decodeURIComponent(routeParam),
      commander: query.get('commander') || '',
    }
  }

  if (routeName === 'deck' && routeParam) {
    return {
      name: HASH_ROUTES.DECK_EDITOR,
      deckId: decodeURIComponent(routeParam),
      panel: normalizeDeckPanel(routePanel),
    }
  }

  if (LEGACY_HASHES[path]) {
    return { name: LEGACY_HASHES[path] }
  }

  return { name: HASH_ROUTES.HOME }
}

export function routeToHash(route = {}) {
  switch (route.name) {
    case HASH_ROUTES.CONTACT:
      return '#/contact'
    case HASH_ROUTES.RELEASE_NOTES:
      return '#/release-notes'
    case HASH_ROUTES.META_ADMIN:
      return '#/meta-admin'
    case HASH_ROUTES.IMPORT:
      return '#/import'
    case HASH_ROUTES.PUBLIC_DECK:
      if (!route.deckId) return ''
      return appendQuery(`#/public/${encodeURIComponent(route.deckId)}`, { commander: route.commander })
    case HASH_ROUTES.DECK_EDITOR:
      if (!route.deckId) return ''
      return `#/deck/${encodeURIComponent(route.deckId)}/${normalizeDeckPanel(route.panel)}`
    default:
      return ''
  }
}

export function pageViewForRoute(route = {}) {
  if (route.name === HASH_ROUTES.CONTACT) return HASH_ROUTES.CONTACT
  if (route.name === HASH_ROUTES.RELEASE_NOTES) return HASH_ROUTES.RELEASE_NOTES
  if (route.name === HASH_ROUTES.META_ADMIN) return HASH_ROUTES.META_ADMIN
  return HASH_ROUTES.HOME
}

export function titleForRoute(route = {}) {
  switch (route.name) {
    case HASH_ROUTES.CONTACT:
      return 'Contato - MTG Deck Manager'
    case HASH_ROUTES.RELEASE_NOTES:
      return 'Novidades - MTG Deck Manager'
    case HASH_ROUTES.META_ADMIN:
      return 'Meta Admin - MTG Deck Manager'
    case HASH_ROUTES.IMPORT:
      return 'Importar Deck - MTG Deck Manager'
    case HASH_ROUTES.PUBLIC_DECK:
      return 'Consultar Deck Publico - MTG Deck Manager'
    case HASH_ROUTES.DECK_EDITOR:
      if (route.panel === 'analysis') return 'Analise do Deck - MTG Deck Manager'
      if (route.panel === 'recommendations') return 'Recomendacoes do Deck - MTG Deck Manager'
      return 'Editar Deck - MTG Deck Manager'
    default:
      return 'Biblioteca de Decks - MTG Deck Manager'
  }
}

function normalizeDeckPanel(panel) {
  if (panel === 'analysis' || panel === 'recommendations') return panel
  return 'edit'
}

function appendQuery(hash, params) {
  const query = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim()) {
      query.set(key, String(value).trim())
    }
  })
  const queryString = query.toString()
  return queryString ? `${hash}?${queryString}` : hash
}
