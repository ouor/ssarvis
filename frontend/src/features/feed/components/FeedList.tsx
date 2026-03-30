import { FeedCard } from './FeedCard'
import type { FeedPostViewModel } from '../types'

type FeedListProps = {
  posts: FeedPostViewModel[]
}

export function FeedList({ posts }: FeedListProps) {
  return (
    <div className="feed-stack">
      {posts.map((post) => (
        <FeedCard key={post.id} post={post} />
      ))}
    </div>
  )
}
