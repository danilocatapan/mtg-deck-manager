const RELEASE_NOTES_URL = `${import.meta.env.BASE_URL}release-notes.json`

export async function getReleaseNotes() {
  const response = await fetch(RELEASE_NOTES_URL, {
    headers: { Accept: 'application/json' },
    cache: 'no-store',
  })

  if (!response.ok) {
    throw new Error('Nao foi possivel carregar as novidades.')
  }

  const notes = await response.json()
  if (!Array.isArray(notes)) {
    throw new Error('Arquivo de novidades invalido.')
  }

  return notes
    .map(normalizeReleaseNote)
    .filter(Boolean)
    .sort(compareReleaseNotes)
}

function normalizeReleaseNote(note) {
  if (!note || !note.version || !note.date || !note.title || !note.changes) {
    return null
  }

  return {
    version: String(note.version),
    date: String(note.date),
    title: String(note.title),
    changes: {
      added: toStringList(note.changes.added),
      changed: toStringList(note.changes.changed),
      fixed: toStringList(note.changes.fixed),
      security: toStringList(note.changes.security),
    },
  }
}

function toStringList(value) {
  return Array.isArray(value) ? value.map(String).filter(Boolean) : []
}

function compareReleaseNotes(a, b) {
  const versionComparison = compareSemver(b.version, a.version)
  if (versionComparison !== 0) return versionComparison
  return b.date.localeCompare(a.date)
}

function compareSemver(left, right) {
  const leftParts = parseSemver(left)
  const rightParts = parseSemver(right)
  for (let index = 0; index < 3; index += 1) {
    if (leftParts[index] !== rightParts[index]) {
      return leftParts[index] - rightParts[index]
    }
  }
  return 0
}

function parseSemver(version) {
  const match = String(version).match(/(\d+)\.(\d+)\.(\d+)/)
  if (!match) return [0, 0, 0]
  return match.slice(1).map(Number)
}
