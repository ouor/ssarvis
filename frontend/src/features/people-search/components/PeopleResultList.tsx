import { PeopleResultCard } from './PeopleResultCard'
import type { ProfileSummary } from '../../profile-settings/types'

type PeopleResultListProps = {
  users: ProfileSummary[]
  busyUserId?: number | null
  onToggleFollow: (user: ProfileSummary) => Promise<void> | void
}

export function PeopleResultList({
  users,
  busyUserId,
  onToggleFollow,
}: PeopleResultListProps) {
  return (
    <div className="stack-md">
      {users.map((user) => (
        <PeopleResultCard
          key={user.userId}
          user={user}
          isSubmitting={busyUserId === user.userId}
          onToggleFollow={onToggleFollow}
        />
      ))}
    </div>
  )
}
