import { apiRequest } from '../../lib/api/client'
import type { ClonePromptResponse, CloneResponse } from './types'

export function getClones(token: string) {
  return apiRequest<CloneResponse[]>('/api/clones?scope=mine', { token })
}

export function saveSystemPrompt(token: string, answer: string) {
  return apiRequest<ClonePromptResponse>('/api/system-prompt', {
    method: 'POST',
    token,
    body: JSON.stringify({
      answers: [
        {
          question: '평소 말투는 어떤 편인가요?',
          answer,
        },
      ],
    }),
  })
}

export function updateCloneVisibility(
  token: string,
  cloneId: number,
  isPublic: boolean,
) {
  return apiRequest<void>(`/api/clones/${cloneId}/visibility`, {
    method: 'PATCH',
    token,
    body: JSON.stringify({ isPublic }),
  })
}
