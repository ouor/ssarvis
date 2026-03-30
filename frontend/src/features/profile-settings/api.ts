import { apiRequest } from '../../lib/api/client'
import type { Visibility } from '../../lib/types/common'
import type { AutoReplyMode, AutoReplySettings, ProfileSummary } from './types'

export function getMyProfile(token: string) {
  return apiRequest<ProfileSummary>('/api/profiles/me', { token })
}

export function getProfile(token: string, profileUserId: number) {
  return apiRequest<ProfileSummary>(`/api/profiles/${profileUserId}`, { token })
}

export function getProfileByUsername(token: string, username: string) {
  return apiRequest<ProfileSummary>(`/api/profiles/by-username/${username}`, {
    token,
  })
}

export function getPublicProfileByUsername(username: string) {
  return apiRequest<ProfileSummary>(`/api/public/profiles/by-username/${username}`)
}

export function updateDisplayName(token: string, displayName: string) {
  return apiRequest<ProfileSummary>('/api/profiles/me', {
    method: 'PATCH',
    token,
    body: JSON.stringify({ displayName }),
  })
}

export function updateVisibility(token: string, visibility: Visibility) {
  return apiRequest<ProfileSummary>('/api/profiles/me/visibility', {
    method: 'PATCH',
    token,
    body: JSON.stringify({ visibility }),
  })
}

export function getAutoReplySettings(token: string) {
  return apiRequest<AutoReplySettings>('/api/profiles/me/auto-reply', { token })
}

export function updateAutoReplySettings(token: string, mode: AutoReplyMode) {
  return apiRequest<AutoReplySettings>('/api/profiles/me/auto-reply', {
    method: 'PATCH',
    token,
    body: JSON.stringify({ mode }),
  })
}
