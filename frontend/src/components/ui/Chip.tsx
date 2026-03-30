import type { HTMLAttributes } from 'react'
import { cn } from '../../lib/utils/cn'

type ChipProps = HTMLAttributes<HTMLSpanElement> & {
  tone?: 'default' | 'accent' | 'warm' | 'success' | 'danger'
}

export function Chip({ className, tone = 'default', ...props }: ChipProps) {
  return (
    <span
      className={cn(
        'ui-chip',
        tone !== 'default' && `ui-chip-${tone}`,
        className,
      )}
      {...props}
    />
  )
}
