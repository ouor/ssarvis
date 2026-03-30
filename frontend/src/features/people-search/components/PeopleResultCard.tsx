import { Avatar } from '../../../components/ui/Avatar'
import { ProfileLink } from '../../../components/shared/ProfileLink'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Chip } from '../../../components/ui/Chip'
import type { ProfileSummary } from '../../profile-settings/types'

type PeopleResultCardProps = {
  user: ProfileSummary
  isSubmitting?: boolean
  onToggleFollow: (user: ProfileSummary) => Promise<void> | void
}

export function PeopleResultCard({
  user,
  isSubmitting = false,
  onToggleFollow,
}: PeopleResultCardProps) {
  return (
    <Card>
      <div className="profile-row">
        <Avatar name={user.displayName} />
        <div className="entity-meta stack-sm">
          <div className="entity-title">
            <ProfileLink username={user.username} className="profile-link-strong">
              <strong>{user.displayName}</strong>
            </ProfileLink>
            <ProfileLink username={user.username} className="meta-line profile-link">
              @{user.username}
            </ProfileLink>
            <Chip tone={user.visibility === 'PUBLIC' ? 'success' : 'warm'}>
              {user.visibility}
            </Chip>
          </div>
          <p className="muted-copy">
            {user.visibility === 'PUBLIC'
              ? '누구나 탐색하고 DM을 시작할 수 있는 공개 프로필입니다.'
              : '팔로우 중인 사용자 중심으로 관계가 이어지는 비공개 프로필입니다.'}
          </p>
        </div>
        <Button
          variant={user.following ? 'ghost' : 'secondary'}
          disabled={isSubmitting}
          onClick={() => onToggleFollow(user)}
        >
          {user.following ? '언팔로우' : '팔로우'}
        </Button>
      </div>
    </Card>
  )
}
