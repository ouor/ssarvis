import type { HTMLAttributes } from 'react'
import { cn } from '../../lib/utils/cn'

export function Panel({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('glass-panel ui-panel', className)} {...props} />
}
