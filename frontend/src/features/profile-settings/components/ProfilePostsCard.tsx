import { Card } from '../../../components/ui/Card'
import { FeedList } from '../../feed/components/FeedList'
import type { FeedPostViewModel } from '../../feed/types'

type ProfilePostsCardProps = {
  posts: FeedPostViewModel[]
  currentUserId?: number
  onEditPost?: (postId: number, content: string) => Promise<boolean> | boolean
  onDeletePost?: (postId: number) => Promise<boolean> | boolean
  emptyCopy: string
}

export function ProfilePostsCard({
  posts,
  currentUserId,
  onEditPost,
  onDeletePost,
  emptyCopy,
}: ProfilePostsCardProps) {
  return (
    <Card className="stack-md">
      <h2 className="section-title">내 게시물</h2>
      {posts.length > 0 ? (
        <FeedList
          posts={posts}
          currentUserId={currentUserId}
          onEditPost={onEditPost}
          onDeletePost={onDeletePost}
        />
      ) : (
        <p className="muted-copy">{emptyCopy}</p>
      )}
    </Card>
  )
}
