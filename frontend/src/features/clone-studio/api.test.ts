import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  apiFetch,
  authExpiredEventName,
  clearStoredAccessToken,
  formatAssetAccessError,
  getStoredAccessToken,
  storeAccessToken,
} from './api'

describe('clone-studio api helpers', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.restoreAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('stores and clears the access token', () => {
    storeAccessToken('token-123')
    expect(getStoredAccessToken()).toBe('token-123')

    clearStoredAccessToken()
    expect(getStoredAccessToken()).toBe('')
  })

  it('adds the bearer token header to authenticated requests', async () => {
    storeAccessToken('jwt-token')

    const fetchSpy = vi
      .spyOn(window, 'fetch')
      .mockResolvedValue(new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } }))

    await apiFetch('/api/auth/me')

    expect(fetchSpy).toHaveBeenCalledTimes(1)
    const [, init] = fetchSpy.mock.calls[0]
    const headers = new Headers(init?.headers)
    expect(headers.get('Authorization')).toBe('Bearer jwt-token')
  })

  it('clears the token and emits auth-expired when a protected request returns 401', async () => {
    storeAccessToken('expired-token')

    const expiredListener = vi.fn()
    window.addEventListener(authExpiredEventName, expiredListener)
    vi.spyOn(window, 'fetch').mockResolvedValue(new Response('', { status: 401 }))

    await apiFetch('/api/clones')

    expect(getStoredAccessToken()).toBe('')
    expect(expiredListener).toHaveBeenCalledTimes(1)

    window.removeEventListener(authExpiredEventName, expiredListener)
  })

  it('converts asset access failures into a friendly retry message', () => {
    expect(formatAssetAccessError('Forbidden', '선택한 클론 또는 목소리로 대화를 시작할 수 없습니다.')).toBe(
      '선택한 클론 또는 목소리로 대화를 시작할 수 없습니다.\n다른 사용자가 자산을 비공개로 전환했거나 현재 계정으로 접근할 수 없습니다.',
    )
  })
})
