import type { HTMLAttributes } from 'react'
import { cn } from '../../lib/utils/cn'

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('glass-card ui-card', className)} {...props} />
}
