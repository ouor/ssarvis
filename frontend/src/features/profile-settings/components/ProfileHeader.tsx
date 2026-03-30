import type { AppUser } from '../../../lib/types/common'
import { Avatar } from '../../../components/ui/Avatar'
import { Card } from '../../../components/ui/Card'
import { Chip } from '../../../components/ui/Chip'

type ProfileHeaderProps = {
  user: AppUser
}

export function ProfileHeader({ user }: ProfileHeaderProps) {
  return (
    <Card>
      <div className="profile-row">
        <Avatar name={user.displayName} size="lg" />
        <div className="stack-sm">
          <div className="entity-title">
            <strong>{user.displayName}</strong>
            <Chip tone={user.visibility === 'PUBLIC' ? 'success' : 'warm'}>
              {user.visibility}
            </Chip>
          </div>
          <div className="meta-line">@{user.username}</div>
          <p className="muted-copy">
            소개와 게시물을 함께 보는 프로필 공간입니다.
          </p>
        </div>
      </div>
    </Card>
  )
}
