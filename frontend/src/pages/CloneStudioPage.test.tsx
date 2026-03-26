import { render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CloneStudioPage from './CloneStudioPage'

describe('CloneStudioPage user-scoped data flow', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('reloads user-owned clones and voices when the signed-in user changes', async () => {
    const fetchSpy = vi.spyOn(window, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return new Response(JSON.stringify([{ question: 'Q1', choices: ['A', 'B'] }]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      const authHeader = input instanceof Request
        ? input.headers.get('Authorization')
        : new Headers(init?.headers).get('Authorization')

      if (url.endsWith('/api/clones') && authHeader === 'Bearer token-user-1') {
        return new Response(
          JSON.stringify([
            {
              cloneId: 101,
              createdAt: '2026-03-26T00:00:00.000Z',
              alias: 'Alpha',
              shortDescription: '첫 번째 사용자 클론',
            },
          ]),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }

      if (url.endsWith('/api/voices') && authHeader === 'Bearer token-user-1') {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      if (url.endsWith('/api/clones') && authHeader === 'Bearer token-user-2') {
        return new Response(
          JSON.stringify([
            {
              cloneId: 202,
              createdAt: '2026-03-26T00:00:00.000Z',
              alias: 'Beta',
              shortDescription: '두 번째 사용자 클론',
            },
          ]),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }

      if (url.endsWith('/api/voices') && authHeader === 'Bearer token-user-2') {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      throw new Error(`Unhandled request: ${url} (${authHeader ?? 'no-auth'})`)
    })

    window.localStorage.setItem('ssarvis.access-token', 'token-user-1')

    const { rerender } = render(
      <CloneStudioPage
        currentUser={{ userId: 1, username: 'user1', displayName: '사용자1' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    expect(await screen.findByText('Alpha', { selector: 'h2' })).toBeInTheDocument()
    expect(screen.getByText('사용자1님의 클론과 목소리만 불러와서, 나만의 대화와 논쟁 흐름을 이어갈 수 있습니다.')).toBeInTheDocument()

    window.localStorage.setItem('ssarvis.access-token', 'token-user-2')

    rerender(
      <CloneStudioPage
        currentUser={{ userId: 2, username: 'user2', displayName: '사용자2' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    expect(await screen.findByText('Beta', { selector: 'h2' })).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.queryByText('Alpha', { selector: 'h2' })).not.toBeInTheDocument()
    })
    expect(screen.getByText('사용자2님의 클론과 목소리만 불러와서, 나만의 대화와 논쟁 흐름을 이어갈 수 있습니다.')).toBeInTheDocument()
    expect(fetchSpy).toHaveBeenCalled()
  })
})
