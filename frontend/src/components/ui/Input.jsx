export default function Input({ label, error, id, ...props }) {
  return (
    <label style={{ display: 'block' }} htmlFor={id}>
      {label && <div style={{ marginBottom: 6 }}>{label}</div>}
      <input id={id} {...props} />
      {error && <div style={{ color: 'red', marginTop: 6 }}>{error}</div>}
    </label>
  )
}
