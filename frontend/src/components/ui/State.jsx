export function Loading({ message = 'Loading...' }) {
  return <div className="loading">{message}</div>
}

export function Empty({ message = 'No data' }) {
  return <div className="loading">{message}</div>
}

export function ErrorState({ message = 'An error occurred' }) {
  return <div style={{ color: 'red' }}>{message}</div>
}

export default {
  Loading,
  Empty,
  ErrorState,
}
