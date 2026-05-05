import React from 'react'

export default function Button({ children, variant = 'primary', loading = false, disabled = false, onClick, type = 'button', className = '', ...props }) {
  const base = 'btn'
  const vclass = variant === 'secondary' ? 'secondary' : variant === 'danger' ? 'danger' : 'primary'
  return (
    <button type={type} className={`${base} ${vclass} ${className}`} onClick={onClick} disabled={disabled || loading} aria-busy={loading} {...props}>
      {loading ? '...' : children}
    </button>
  )
}
