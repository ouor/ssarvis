import { Avatar } from '../../../components/ui/Avatar'
import { Card } from '../../../components/ui/Card'
import type { FeedPostViewModel } from '../types'

type FeedCardProps = {
  post: FeedPostViewModel
}

export function FeedCard({ post }: FeedCardProps) {
  return (
    <Card>
      <div className="entity-row">
        <Avatar name={post.author.displayName} />
        <div className="entity-meta stack-sm">
          <div className="entity-title">
            <strong>{post.author.displayName}</strong>
            <span className="meta-line">@{post.author.username}</span>
          </div>
          <span className="meta-line">{post.postedAt}</span>
          <p className="muted-copy">{post.content}</p>
        </div>
      </div>
    </Card>
  )
}
