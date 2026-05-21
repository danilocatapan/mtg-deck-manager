const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || ''
const TOKEN_KEY = 'mtg_google_id_token'
const PROFILE_KEY = 'mtg_google_profile'
const AUTH_EVENT = 'mtg-auth-change'
const TOKEN_EXPIRY_SKEW_MS = 60_000
const GOOGLE_ISSUERS = new Set(['accounts.google.com', 'https://accounts.google.com'])

function storage() {
  return window.sessionStorage
}

function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1]
    const normalized = padBase64(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(window.atob(normalized))
  } catch {
    return null
  }
}

export function getAuthToken() {
  clearLegacyPersistentAuth()
  const token = storage().getItem(TOKEN_KEY)
  const profile = decodeJwtPayload(token || '')
  if (!isValidGoogleProfile(profile)) {
    if (token) {
      console.info('event=auth.session.invalid_or_expired')
    }
    clearAuthToken()
    return null
  }
  return token
}

export function getAuthProfile() {
  clearLegacyPersistentAuth()
  const raw = storage().getItem(PROFILE_KEY)
  if (!raw) return null
  try {
    const profile = JSON.parse(raw)
    if (!isValidGoogleProfile(profile)) {
      console.info('event=auth.profile.invalid_or_expired')
      clearAuthToken()
      return null
    }
    return profile
  } catch {
    return null
  }
}

export function setAuthToken(token) {
  const profile = decodeJwtPayload(token)
  if (!isValidGoogleProfile(profile)) {
    throw new Error('Invalid Google credential')
  }
  clearLegacyPersistentAuth()
  storage().setItem(TOKEN_KEY, token)
  storage().setItem(PROFILE_KEY, JSON.stringify(publicProfile(profile)))
  console.info('event=auth.session.stored')
  window.dispatchEvent(new Event(AUTH_EVENT))
}

export function clearAuthToken() {
  const hadSession = Boolean(storage().getItem(TOKEN_KEY) || storage().getItem(PROFILE_KEY))
  storage().removeItem(TOKEN_KEY)
  storage().removeItem(PROFILE_KEY)
  clearLegacyPersistentAuth()
  if (hadSession) {
    console.info('event=auth.session.cleared')
    window.dispatchEvent(new Event(AUTH_EVENT))
  }
}

function clearLegacyPersistentAuth() {
  window.localStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(PROFILE_KEY)
}

export function subscribeAuth(listener) {
  window.addEventListener(AUTH_EVENT, listener)
  window.addEventListener('storage', listener)
  return () => {
    window.removeEventListener(AUTH_EVENT, listener)
    window.removeEventListener('storage', listener)
  }
}

export function getGoogleClientId() {
  return GOOGLE_CLIENT_ID
}

function isValidGoogleProfile(profile) {
  if (!profile?.sub || !profile?.exp) {
    return false
  }
  if (profile.exp * 1000 <= Date.now() + TOKEN_EXPIRY_SKEW_MS) {
    return false
  }
  if (profile.iss && !GOOGLE_ISSUERS.has(profile.iss)) {
    return false
  }
  if (GOOGLE_CLIENT_ID && profile.aud !== GOOGLE_CLIENT_ID) {
    return false
  }
  return true
}

function publicProfile(profile) {
  return {
    sub: profile.sub,
    email: profile.email || null,
    name: profile.name || null,
    picture: profile.picture || null,
    exp: profile.exp,
    iat: profile.iat || null,
    iss: profile.iss || null,
    aud: profile.aud || null,
  }
}

function padBase64(value) {
  const remainder = value.length % 4
  return remainder === 0 ? value : `${value}${'='.repeat(4 - remainder)}`
}
