export default function Input({ label, error, id, ...props }) {
  return (
    <label className="field-label" htmlFor={id}>
      {label && <div className="field-label-text">{label}</div>}
      <input id={id} {...props} />
      {error && <div className="field-error">{error}</div>}
    </label>
  )
}
