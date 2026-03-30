import { apiRequest } from '../../lib/api/client'
import type { FeedPostResponse } from './types'

export function getFeed(token: string) {
  return apiRequest<FeedPostResponse[]>('/api/posts/feed', { token })
}

export function getPost(token: string, postId: number) {
  return apiRequest<FeedPostResponse>(`/api/posts/${postId}`, { token })
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

export function getProfilePosts(token: string, profileUserId: number) {
  return apiRequest<FeedPostResponse[]>(`/api/profiles/${profileUserId}/posts`, {
    token,
  })
}

export function getPublicProfilePosts(username: string) {
  return apiRequest<FeedPostResponse[]>(`/api/public/profiles/${username}/posts`)
}

export function updatePost(token: string, postId: number, content: string) {
  return apiRequest<FeedPostResponse>(`/api/posts/${postId}`, {
    method: 'PATCH',
    token,
    body: JSON.stringify({ content }),
  })
}

export function deletePost(token: string, postId: number) {
  return apiRequest<void>(`/api/posts/${postId}`, {
    method: 'DELETE',
    token,
  })
}
