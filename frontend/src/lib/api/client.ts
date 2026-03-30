const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number
  data: unknown

  constructor(status: number, message: string, data?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

async function readErrorPayload(response: Response) {
  const contentType = response.headers.get('Content-Type') ?? ''

  if (contentType.includes('application/json')) {
    const data = await response.json()
    const message =
      typeof data === 'object' &&
      data !== null &&
      'message' in data &&
      typeof data.message === 'string'
        ? data.message
        : `Request failed: ${response.status}`

    return { message, data }
  }

  const text = await response.text()

  return {
    message: text || `Request failed: ${response.status}`,
    data: text || null,
  }
}

type RequestOptions = RequestInit & {
  token?: string | null
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
) {
  const headers = new Headers(options.headers)
  const isFormData =
    typeof FormData !== 'undefined' && options.body instanceof FormData

  if (!headers.has('Content-Type') && options.body && !isFormData) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.token) {
    headers.set('Authorization', `Bearer ${options.token}`)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    const { message, data } = await readErrorPayload(response)
    throw new ApiError(response.status, message, data)
  }

  if (response.status === 204) {
    return null as T
  }

  return (await response.json()) as T
}
