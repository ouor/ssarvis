import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { buildUserProfileRoute } from '../../lib/constants/routes'

type ProfileLinkProps = {
  username: string
  className?: string
  children: ReactNode
}

export function ProfileLink({
  username,
  className,
  children,
}: ProfileLinkProps) {
  return (
    <Link to={buildUserProfileRoute(username)} className={className}>
      {children}
    </Link>
  )
}
