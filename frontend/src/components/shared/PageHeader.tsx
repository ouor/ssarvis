import type { ReactNode } from 'react'

type PageHeaderProps = {
  eyebrow: string
  title: string
  subtitle: string
  actions?: ReactNode
}

export function PageHeader({
  eyebrow,
  title,
  subtitle,
  actions,
}: PageHeaderProps) {
  return (
    <header className="page-header">
      <div className="page-title-wrap">
        <span className="page-eyebrow">{eyebrow}</span>
        <h1 className="page-title">{title}</h1>
        <p className="page-subtitle">{subtitle}</p>
      </div>
      {actions}
    </header>
  )
}
