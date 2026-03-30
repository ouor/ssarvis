import { apiRequest } from '../../lib/api/client'
import type {
  DmMessageResponse,
  DmThreadDetailResponse,
  DmThreadResponse,
} from './types'

export function getThreads(token: string) {
  return apiRequest<DmThreadResponse[]>('/api/dms/threads', { token })
}

export function getThread(token: string, threadId: number) {
  return apiRequest<DmThreadDetailResponse>(`/api/dms/threads/${threadId}`, {
    token,
  })
}

export function sendTextMessage(
  token: string,
  threadId: number,
  content: string,
) {
  return apiRequest<DmMessageResponse>(`/api/dms/threads/${threadId}/messages`, {
    method: 'POST',
    token,
    body: JSON.stringify({ content }),
  })
}

export function uploadVoiceMessage(
  token: string,
  threadId: number,
  audio: File,
) {
  const formData = new FormData()
  formData.append('audio', audio)

  return apiRequest<DmMessageResponse>(
    `/api/dms/threads/${threadId}/voice-messages`,
    {
      method: 'POST',
      token,
      body: formData,
    },
  )
}
