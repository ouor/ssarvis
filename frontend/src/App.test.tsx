import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { authExpiredEventName } from './features/clone-studio/api'

vi.mock('./pages/CloneStudioPage', () => ({
  default: ({
    currentUser,
    deactivating,
    onDeactivate,
    onLogout,
  }: {
    currentUser: { displayName: string; username: string }
    deactivating: boolean
    onDeactivate: () => Promise<void>
    onLogout: () => void
  }) => (
    <section>
      <h1>studio</h1>
      <p>{currentUser.displayName}</p>
      <p>@{currentUser.username}</p>
      <button onClick={onLogout} type="button">
        로그아웃
      </button>
      <button disabled={deactivating} onClick={() => void onDeactivate()} type="button">
        {deactivating ? '탈퇴 처리 중...' : '회원 탈퇴'}
      </button>
    </section>
  ),
}))

describe('App auth flow', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.restoreAllMocks()
  })

  it('restores the current user from a stored access token on startup', async () => {
    window.localStorage.setItem('ssarvis.access-token', 'saved-token')

    const fetchSpy = vi.spyOn(window, 'fetch').mockImplementation(async (_input, init) => {
      const headers = new Headers(init?.headers)
      expect(headers.get('Authorization')).toBe('Bearer saved-token')

      return new Response(
        JSON.stringify({
          userId: 7,
          username: 'mira',
          displayName: 'Mira',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      )
    })

    render(<App />)

    expect(await screen.findByText('studio')).toBeInTheDocument()
    expect(screen.getByText('Mira')).toBeInTheDocument()
    expect(screen.getByText('@mira')).toBeInTheDocument()
    expect(fetchSpy).toHaveBeenCalledTimes(1)
  })

  it('logs in and stores the received access token', async () => {
    const user = userEvent.setup()
    const fetchSpy = vi.spyOn(window, 'fetch')

    fetchSpy.mockResolvedValueOnce(new Response('', { status: 401 }))
    fetchSpy.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          userId: 11,
          username: 'juno',
          displayName: 'Juno',
          visibility: 'PUBLIC',
          accessToken: 'new-token',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )

    render(<App />)

    expect(await screen.findByText('다시 돌아오신 걸 환영해요.')).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText('아이디를 입력하세요'), 'juno')
    await user.type(screen.getByPlaceholderText('비밀번호를 입력하세요'), 'pw-1234')
    await user.click(screen.getByText('로그인', { selector: 'button.auth-submit' }))

    expect(await screen.findByText('studio')).toBeInTheDocument()
    expect(screen.getByText('Juno')).toBeInTheDocument()
    expect(window.localStorage.getItem('ssarvis.access-token')).toBe('new-token')

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledTimes(2)
    })
  })

  it('returns to the login screen when the auth-expired event fires', async () => {
    window.localStorage.setItem('ssarvis.access-token', 'saved-token')
    vi.spyOn(window, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          userId: 21,
          username: 'lane',
          displayName: 'Lane',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )

    render(<App />)

    expect(await screen.findByText('studio')).toBeInTheDocument()

    fireEvent(window, new CustomEvent(authExpiredEventName))

    expect(await screen.findByText('세션이 만료되어 다시 로그인해야 합니다.', { selector: 'p.auth-error' })).toBeInTheDocument()
    expect(screen.getByText('로그인', { selector: 'button.auth-submit' })).toBeInTheDocument()
  })

  it('soft deletes the current account and logs out automatically', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'saved-token')

    vi.spyOn(window, 'confirm').mockReturnValue(true)

    const fetchSpy = vi.spyOn(window, 'fetch')
    fetchSpy.mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          userId: 25,
          username: 'sora',
          displayName: 'Sora',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )
    fetchSpy.mockResolvedValueOnce(new Response(null, { status: 204 }))

    render(<App />)

    expect(await screen.findByText('studio')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '회원 탈퇴' }))

    expect(await screen.findByText('회원 탈퇴가 완료되었습니다. 다시 로그인할 수 없습니다.', { selector: 'p.auth-error' })).toBeInTheDocument()
    expect(screen.getByText('로그인', { selector: 'button.auth-submit' })).toBeInTheDocument()
    expect(window.localStorage.getItem('ssarvis.access-token')).toBeNull()

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledTimes(2)
    })
  })
})
