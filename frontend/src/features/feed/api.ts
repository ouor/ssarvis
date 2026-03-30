import { apiRequest } from '../../lib/api/client'
import type { FeedPostResponse } from './types'

export function getFeed(token: string) {
  return apiRequest<FeedPostResponse[]>('/api/posts/feed', { token })
}

export function createPost(token: string, content: string) {
  return apiRequest<FeedPostResponse>('/api/posts', {
    method: 'POST',
    token,
    body: JSON.stringify({ content }),
  })
}

export function getMyPosts(token: string) {
  return apiRequest<FeedPostResponse[]>('/api/profiles/me/posts', { token })
}
