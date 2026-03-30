import { apiRequest } from '../../lib/api/client'
import type { VoiceResponse } from './types'

export function getVoices(token: string) {
  return apiRequest<VoiceResponse[]>('/api/voices?scope=mine', { token })
}

export function uploadVoice(
  token: string,
  sample: File,
  alias?: string,
) {
  const formData = new FormData()
  formData.append('sample', sample)

  if (alias?.trim()) {
    formData.append('alias', alias.trim())
  }

  return apiRequest<VoiceResponse>('/api/voices', {
    method: 'POST',
    token,
    body: formData,
  })
}

export function updateVoiceVisibility(
  token: string,
  registeredVoiceId: number,
  isPublic: boolean,
) {
  return apiRequest<void>(`/api/voices/${registeredVoiceId}/visibility`, {
    method: 'PATCH',
    token,
    body: JSON.stringify({ isPublic }),
  })
}
