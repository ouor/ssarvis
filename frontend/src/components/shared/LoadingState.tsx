type LoadingStateProps = {
  title?: string
  copy?: string
}

export function LoadingState({
  title = '불러오는 중입니다',
  copy = '잠시만 기다려주세요.',
}: LoadingStateProps) {
  return (
    <div className="glass-card empty-state loading-state" role="status" aria-live="polite">
      <div className="loading-orb" aria-hidden="true" />
      <h2 className="section-title">{title}</h2>
      <p className="muted-copy">{copy}</p>
    </div>
  )
}
