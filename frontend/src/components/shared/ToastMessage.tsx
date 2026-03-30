type ToastMessageProps = {
  title: string
  copy: string
}

export function ToastMessage({ title, copy }: ToastMessageProps) {
  return (
    <div className="toast-message glass-card" role="status" aria-live="polite">
      <strong>{title}</strong>
      <p className="muted-copy">{copy}</p>
    </div>
  )
}
