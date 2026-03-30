type EmptyStateProps = {
  title: string
  copy: string
}

export function EmptyState({ title, copy }: EmptyStateProps) {
  return (
    <div className="glass-card empty-state">
      <h2 className="section-title">{title}</h2>
      <p className="muted-copy">{copy}</p>
    </div>
  )
}
