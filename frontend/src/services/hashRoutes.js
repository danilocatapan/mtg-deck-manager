export const HASH_ROUTES = {
  HOME: 'home',
  CONTACT: 'contact',
  RELEASE_NOTES: 'release-notes',
  META_ADMIN: 'meta-admin',
  IMPORT: 'import',
  PUBLIC_DECK: 'public-deck',
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

  const [routeName, routeParam] = value.split('/').filter(Boolean)
  if (routeName === 'public' && routeParam) {
    return { name: HASH_ROUTES.PUBLIC_DECK, deckId: decodeURIComponent(routeParam) }
  }

  if (LEGACY_HASHES[value]) {
    return { name: LEGACY_HASHES[value] }
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
      return route.deckId ? `#/public/${encodeURIComponent(route.deckId)}` : ''
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
    default:
      return 'Biblioteca de Decks - MTG Deck Manager'
  }
}
