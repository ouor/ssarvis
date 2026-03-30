import type { Visibility } from '../../lib/types/common'

export type FeedPostResponse = {
  postId: number
  ownerUserId: number
  ownerUsername: string
  ownerDisplayName: string
  ownerVisibility: Visibility
  content: string
  createdAt: string
}

export type FeedPostViewModel = {
  id: number
  author: {
    userId: number
    username: string
    displayName: string
    visibility: Visibility
  }
  postedAt: string
  createdAt: string
  content: string
}
