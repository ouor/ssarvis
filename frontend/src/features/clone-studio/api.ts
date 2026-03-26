import type { ApiErrorResponse, StreamEvent } from './types'

export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
export const questionAssetPath = `${import.meta.env.BASE_URL}questions.json`
export const authExpiredEventName = 'ssarvis:auth-expired'
const accessTokenStorageKey = 'ssarvis.access-token'

export function getStoredAccessToken() {
  return window.localStorage.getItem(accessTokenStorageKey) ?? ''
}

export function storeAccessToken(accessToken: string) {
  window.localStorage.setItem(accessTokenStorageKey, accessToken)
}

export function clearStoredAccessToken() {
  window.localStorage.removeItem(accessTokenStorageKey)
}

export async function apiFetch(input: string, init?: RequestInit) {
  const headers = new Headers(init?.headers)
  const accessToken = getStoredAccessToken()

  if (accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const response = await fetch(input, {
    ...init,
    headers,
  })

  if (response.status === 401 && accessToken) {
    clearStoredAccessToken()
    window.dispatchEvent(new CustomEvent(authExpiredEventName))
  }

  return response
}

export async function readErrorMessage(response: Response, fallbackMessage: string) {
  const contentType = response.headers.get('Content-Type') ?? ''

  if (contentType.includes('application/json')) {
    try {
      const errorBody: ApiErrorResponse = await response.json()
      const details = errorBody.details?.filter((detail) => detail.trim().length > 0) ?? []
      if (details.length > 0) {
        return [errorBody.message, ...details].filter(Boolean).join('\n')
      }
      if (errorBody.message?.trim()) {
        return errorBody.message
      }
    } catch {
      return fallbackMessage
    }
  }

  try {
    const text = (await response.text()).trim()
    return text || fallbackMessage
  } catch {
    return fallbackMessage
  }
}

export function formatAssetAccessError(message: string, fallbackMessage: string) {
  const trimmed = message.trim()
  const lowered = trimmed.toLowerCase()
  const looksLikeAccessIssue =
    lowered.includes('(403)') ||
    lowered.includes('(404)') ||
    lowered.includes('forbidden') ||
    lowered.includes('not found') ||
    trimmed.includes('권한') ||
    trimmed.includes('접근') ||
    trimmed.includes('비공개')

  if (!looksLikeAccessIssue) {
    return trimmed || fallbackMessage
  }

  return `${fallbackMessage}\n다른 사용자가 자산을 비공개로 전환했거나 현재 계정으로 접근할 수 없습니다.`
}

export async function readNdjsonStream(response: Response, onEvent: (event: StreamEvent) => Promise<void> | void) {
  if (!response.body) {
    throw new Error('스트림 응답 본문이 비어 있습니다.')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''

    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed) {
        continue
      }
      await onEvent(JSON.parse(trimmed) as StreamEvent)
    }
  }

  if (buffer.trim()) {
    await onEvent(JSON.parse(buffer) as StreamEvent)
  }
}
