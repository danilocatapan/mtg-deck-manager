const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || ''
const TOKEN_KEY = 'mtg_google_id_token'
const PROFILE_KEY = 'mtg_google_profile'
const AUTH_EVENT = 'mtg-auth-change'

function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(window.atob(normalized))
  } catch {
    return null
  }
}

export function getAuthToken() {
  const token = window.localStorage.getItem(TOKEN_KEY)
  const profile = getAuthProfile()
  if (!token || !profile?.exp || profile.exp * 1000 <= Date.now()) {
    clearAuthToken()
    return null
  }
  return token
}

export function getAuthProfile() {
  const raw = window.localStorage.getItem(PROFILE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function setAuthToken(token) {
  const profile = decodeJwtPayload(token)
  if (!profile?.sub) {
    throw new Error('Invalid Google credential')
  }
  window.localStorage.setItem(TOKEN_KEY, token)
  window.localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
  window.dispatchEvent(new Event(AUTH_EVENT))
}

export function clearAuthToken() {
  window.localStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(PROFILE_KEY)
  window.dispatchEvent(new Event(AUTH_EVENT))
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
