import { apiRequest } from '../../lib/api/client'
import type { ProfileSummary } from '../profile-settings/types'

export function searchUsers(token: string, keyword: string) {
  const query = new URLSearchParams({ query: keyword })
  return apiRequest<ProfileSummary[]>(`/api/follows/users/search?${query.toString()}`, {
    token,
  })
}

export function getFollowingUsers(token: string) {
  return apiRequest<ProfileSummary[]>('/api/follows', { token })
}

export function followUser(token: string, targetUserId: number) {
  return apiRequest<ProfileSummary>(`/api/follows/${targetUserId}`, {
    method: 'POST',
    token,
  })
}

export function unfollowUser(token: string, targetUserId: number) {
  return apiRequest<void>(`/api/follows/${targetUserId}`, {
    method: 'DELETE',
    token,
  })
}
