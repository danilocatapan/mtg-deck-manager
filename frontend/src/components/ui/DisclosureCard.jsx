import { useId, useState } from 'react'

export default function DisclosureCard({
  title,
  summary,
  meta = null,
  children,
  className = '',
  defaultOpen = true,
}) {
  const [open, setOpen] = useState(defaultOpen)
  const contentId = useId()

  return (
    <article className={`${className} ${open ? '' : 'is-collapsed'}`}>
      <header className="recommendation-card-header">
        <button
          type="button"
          className="disclosure-card-trigger"
          aria-expanded={open}
          aria-controls={contentId}
          onClick={() => setOpen((current) => !current)}
        >
          <span className="disclosure-card-title">{title}</span>
          {summary && <span className="disclosure-card-summary">{summary}</span>}
          <span className="disclosure-card-indicator" aria-hidden="true">{open ? '-' : '+'}</span>
        </button>
        {open && meta}
      </header>
      {open && (
        <div id={contentId} className="disclosure-card-content">
          {children}
        </div>
      )}
    </article>
  )
}
