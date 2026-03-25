import type { ApiErrorResponse, StreamEvent } from './types'

export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
export const questionAssetPath = `${import.meta.env.BASE_URL}questions.json`

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
