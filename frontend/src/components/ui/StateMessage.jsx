export default function StateMessage({ tone = 'neutral', title, children }) {
  return (
    <div className={`state-message ${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      {title && <strong>{title}</strong>}
      {children && <p>{children}</p>}
    </div>
  )
}
