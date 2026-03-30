import type { ReactNode } from 'react'

type ContextPanelProps = {
  children: ReactNode
}

export function ContextPanel({ children }: ContextPanelProps) {
  return (
    <aside className="context-region">
      <div className="context-region-inner">{children}</div>
    </aside>
  )
}
