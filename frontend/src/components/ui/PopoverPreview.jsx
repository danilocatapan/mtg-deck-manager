import { useId, useRef, useState } from 'react'

export default function PopoverPreview({ label, buttonLabel, children, onOpen }) {
  const [open, setOpen] = useState(false)
  const closeTimerRef = useRef(null)
  const popoverId = useId()

  function openPreview() {
    window.clearTimeout(closeTimerRef.current)
    if (!open) {
      setOpen(true)
      onOpen?.()
    }
  }

  function scheduleClose() {
    window.clearTimeout(closeTimerRef.current)
    closeTimerRef.current = window.setTimeout(() => setOpen(false), 120)
  }

  function closePreview() {
    window.clearTimeout(closeTimerRef.current)
    setOpen(false)
  }

  function handleKeyDown(event) {
    if (event.key === 'Escape') {
      closePreview()
    }
  }

  return (
    <span
      className={`card-name-preview ${open ? 'is-open' : ''}`}
      onPointerEnter={openPreview}
      onPointerLeave={scheduleClose}
      onFocus={openPreview}
      onBlur={scheduleClose}
      onKeyDown={handleKeyDown}
    >
      <button
        type="button"
        className="card-name-trigger"
        aria-label={buttonLabel}
        aria-describedby={open ? popoverId : undefined}
        aria-expanded={open}
        onClick={open ? closePreview : openPreview}
      >
        {label}
      </button>
      {open && (
        <span
          id={popoverId}
          className="card-name-popover"
          role="tooltip"
          onPointerEnter={openPreview}
          onPointerLeave={scheduleClose}
        >
          {children}
        </span>
      )}
    </span>
  )
}
