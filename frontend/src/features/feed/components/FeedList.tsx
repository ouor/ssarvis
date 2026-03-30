import { FeedCard } from './FeedCard'
import type { FeedPostViewModel } from '../types'

type FeedListProps = {
  posts: FeedPostViewModel[]
  currentUserId?: number
  onEditPost?: (postId: number, content: string) => Promise<boolean> | boolean
  onDeletePost?: (postId: number) => Promise<boolean> | boolean
}

export function FeedList({
  posts,
  currentUserId,
  onEditPost,
  onDeletePost,
}: FeedListProps) {
  return (
    <div className="feed-stack">
      {posts.map((post) => (
        <FeedCard
          key={post.id}
          post={post}
          canManage={post.author.userId === currentUserId}
          onEditPost={onEditPost}
          onDeletePost={onDeletePost}
        />
      ))}
    </div>
  )
}
