export default function FloatingActionBar({ actions = [], label = 'Acoes rapidas' }) {
  const visibleActions = actions.filter(Boolean)
  if (visibleActions.length === 0) return null

  return (
    <nav className="mobile-quick-nav floating-action-bar" aria-label={label}>
      {visibleActions.map((action) => (
        <button
          key={action.label}
          type="button"
          onClick={action.onClick}
          disabled={action.disabled}
          aria-label={action.ariaLabel || action.label}
        >
          {action.icon && <img className="floating-action-icon" src={action.icon} alt="" aria-hidden="true" />}
          <span>{action.label}</span>
        </button>
      ))}
    </nav>
  )
}
