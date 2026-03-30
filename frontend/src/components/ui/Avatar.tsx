import { cn } from '../../lib/utils/cn'

type AvatarProps = {
  name: string
  size?: 'sm' | 'md' | 'lg'
}

export function Avatar({ name, size = 'md' }: AvatarProps) {
  return (
    <span
      className={cn('ui-avatar', size !== 'md' && `ui-avatar-${size}`)}
      aria-hidden="true"
    >
      {name.slice(0, 1)}
    </span>
  )
}
