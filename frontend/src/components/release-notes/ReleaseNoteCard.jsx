const CHANGE_GROUPS = [
  ['added', 'Added'],
  ['changed', 'Changed'],
  ['fixed', 'Fixed'],
  ['security', 'Security'],
]

export default function ReleaseNoteCard({ note }) {
  const visibleGroups = CHANGE_GROUPS.filter(([key]) => note.changes[key]?.length)

  return (
    <article className="release-note-card">
      <header className="release-note-header">
        <div>
          <p className="eyebrow">v{note.version}</p>
          <h2>{note.title}</h2>
        </div>
        <time dateTime={note.date}>{formatDate(note.date)}</time>
      </header>

      <div className="release-note-badges" aria-label="Categorias da release">
        {visibleGroups.map(([key, label]) => (
          <span key={key} className={`release-badge ${key}`}>{label}</span>
        ))}
      </div>

      <div className="release-note-sections">
        {visibleGroups.map(([key, label]) => (
          <section key={key}>
            <h3>{label}</h3>
            <ul>
              {note.changes[key].map((change) => (
                <li key={change}>{change}</li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </article>
  )
}

function formatDate(date) {
  const parsed = new Date(`${date}T00:00:00Z`)
  if (Number.isNaN(parsed.getTime())) return date
  return new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' }).format(parsed)
}
