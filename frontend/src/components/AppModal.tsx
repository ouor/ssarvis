import type { ReactNode } from 'react'

type AppModalProps = {
  title: string
  subtitle?: string
  onClose: () => void
  onBack?: () => void
  children: ReactNode
}

function AppModal({ title, subtitle, onClose, onBack, children }: AppModalProps) {
  return (
    <div className="modal-shell" role="dialog" aria-modal="true">
      <div className="modal-backdrop" onClick={onClose} />
      <section className="modal-card">
        <header className="modal-header">
          <div className="modal-header-actions">
            {onBack ? (
              <button className="icon-button" onClick={onBack} type="button">
                ←
              </button>
            ) : (
              <span className="icon-button icon-button-ghost" />
            )}
            <button className="icon-button" onClick={onClose} type="button">
              ✕
            </button>
          </div>
          <div className="modal-copy">
            <p className="modal-kicker">Kindred Flow</p>
            <h2>{title}</h2>
            {subtitle ? <p className="modal-subtitle">{subtitle}</p> : null}
          </div>
        </header>
        <div className="modal-body">{children}</div>
      </section>
    </div>
  )
}

export default AppModal
