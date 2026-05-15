export default function StateMessage({ tone = 'neutral', title, children }) {
  const role = tone === 'error' ? 'alert' : 'status'
  return (
    <div className={`state-message ${tone}`} role={role} aria-live={tone === 'error' ? 'assertive' : 'polite'}>
      {title && <strong>{title}</strong>}
      {children && <p>{children}</p>}
    </div>
  )
}
