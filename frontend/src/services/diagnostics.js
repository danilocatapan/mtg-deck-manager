const ENABLED_KEY = 'mtg_diagnostics_enabled'
const EVENTS_KEY = 'mtg_diagnostics_events'
const EVENT_NAME = 'mtg-diagnostics-change'
const MAX_EVENTS = 30

function storage() {
  return typeof window === 'undefined' ? null : window.sessionStorage
}

export function diagnosticsEnabled() {
  return storage()?.getItem(ENABLED_KEY) === 'true'
}

export function setDiagnosticsEnabled(enabled) {
  storage()?.setItem(ENABLED_KEY, String(Boolean(enabled)))
  window.dispatchEvent(new Event(EVENT_NAME))
}

export function diagnosticEvents() {
  try {
    return JSON.parse(storage()?.getItem(EVENTS_KEY) || '[]')
  } catch {
    return []
  }
}

export function emitDiagnostic(event, details = {}) {
  if (!diagnosticsEnabled()) return
  const safe = {
    event,
    at: new Date().toISOString(),
    auditId: details.auditId || null,
    runId: details.runId || null,
    bracket: details.bracket || null,
    confidence: details.confidence || null,
    status: details.status || null,
    count: Number.isFinite(details.count) ? details.count : null,
    sourceCount: Array.isArray(details.sources) ? details.sources.length : null,
    limitationCount: Array.isArray(details.limitations) ? details.limitations.length : null,
  }
  const events = [...diagnosticEvents(), safe].slice(-MAX_EVENTS)
  storage()?.setItem(EVENTS_KEY, JSON.stringify(events))
  console.info(event, safe)
  window.dispatchEvent(new Event(EVENT_NAME))
}

export function subscribeDiagnostics(listener) {
  window.addEventListener(EVENT_NAME, listener)
  return () => window.removeEventListener(EVENT_NAME, listener)
}
