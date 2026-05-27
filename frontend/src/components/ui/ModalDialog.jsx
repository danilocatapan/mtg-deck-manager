import { useEffect, useRef } from 'react'

const FOCUSABLE_SELECTOR = [
  'button:not([disabled])',
  '[href]',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

export default function ModalDialog({
  open,
  onClose,
  labelledBy,
  describedBy,
  className = '',
  children,
  closeOnBackdrop = true,
  initialFocusRef = null,
}) {
  const dialogRef = useRef(null)
  const previousFocusRef = useRef(null)

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog || !open) return undefined

    previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null

    if (!dialog.open) {
      dialog.showModal()
    }

    queueMicrotask(() => {
      const explicitTarget = initialFocusRef?.current
      const fallbackTarget = dialog.querySelector('[data-autofocus]') || dialog.querySelector(FOCUSABLE_SELECTOR) || dialog
      const target = explicitTarget || fallbackTarget
      if (target instanceof HTMLElement) {
        target.focus({ preventScroll: true })
      }
    })

    return () => {
      if (dialog.open) {
        dialog.close()
      }
      const previousFocus = previousFocusRef.current
      if (previousFocus?.isConnected) {
        previousFocus.focus({ preventScroll: true })
      }
    }
  }, [initialFocusRef, open])

  if (!open) return null

  function handleCancel(event) {
    event.preventDefault()
    onClose?.()
  }

  function handleClick(event) {
    if (closeOnBackdrop && event.target === dialogRef.current) {
      onClose?.()
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className={`modal-dialog ${className}`}
      aria-modal="true"
      aria-labelledby={labelledBy}
      aria-describedby={describedBy}
      onCancel={handleCancel}
      onClick={handleClick}
    >
      {children}
    </dialog>
  )
}
