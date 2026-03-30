import type { TextareaHTMLAttributes } from 'react'
import { cn } from '../../lib/utils/cn'

export function Textarea({
  className,
  ...props
}: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cn('ui-textarea', className)} {...props} />
}
