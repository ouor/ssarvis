import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CloneStudioPage from './CloneStudioPage'

const playerSpies = vi.hoisted(() => ({
  configure: vi.fn<(sampleRate: number, channels: number) => void>(),
  appendBase64Chunk: vi.fn<(base64Chunk: string) => Promise<void>>(async () => {}),
  finish: vi.fn<() => Promise<void>>(async () => {}),
  buildWavUrl: vi.fn<() => string>(() => 'blob:mock-audio'),
  dispose: vi.fn<() => Promise<void>>(async () => {}),
}))

vi.mock('../utils/PcmStreamPlayer', () => ({
  default: class MockPcmStreamPlayer {
    configure(sampleRate: number, channels: number) {
      return playerSpies.configure(sampleRate, channels)
    }

    appendBase64Chunk(base64Chunk: string) {
      return playerSpies.appendBase64Chunk(base64Chunk)
    }

    finish() {
      return playerSpies.finish()
    }

    buildWavUrl() {
      return playerSpies.buildWavUrl()
    }

    dispose() {
      return playerSpies.dispose()
    }
  },
}))

describe('CloneStudioPage user-scoped data flow', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    playerSpies.configure.mockClear()
    playerSpies.appendBase64Chunk.mockClear()
    playerSpies.finish.mockClear()
    playerSpies.buildWavUrl.mockClear()
    playerSpies.dispose.mockClear()
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

      if (url.includes('/api/clones?scope=mine') && authHeader === 'Bearer token-user-1') {
        return new Response(
          JSON.stringify([
            {
              cloneId: 101,
              createdAt: '2026-03-26T00:00:00.000Z',
              alias: 'Alpha',
              shortDescription: '첫 번째 사용자 클론',
              isPublic: false,
              ownerDisplayName: '사용자1',
            },
          ]),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }

      if (url.includes('/api/clones?scope=public') && authHeader === 'Bearer token-user-1') {
        return jsonResponse([])
      }

      if (url.includes('/api/clones?scope=friend') && authHeader === 'Bearer token-user-1') {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=mine') && authHeader === 'Bearer token-user-1') {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      if (url.includes('/api/voices?scope=public') && authHeader === 'Bearer token-user-1') {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      if (url.includes('/api/voices?scope=friend') && authHeader === 'Bearer token-user-1') {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      if (url.endsWith('/api/chat/conversations') && authHeader === 'Bearer token-user-1') {
        return jsonResponse([])
      }

      if (url.endsWith('/api/debates') && authHeader === 'Bearer token-user-1') {
        return jsonResponse([])
      }

      if (url.includes('/api/clones?scope=mine') && authHeader === 'Bearer token-user-2') {
        return new Response(
          JSON.stringify([
            {
              cloneId: 202,
              createdAt: '2026-03-26T00:00:00.000Z',
              alias: 'Beta',
              shortDescription: '두 번째 사용자 클론',
              isPublic: false,
              ownerDisplayName: '사용자2',
            },
          ]),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        )
      }

      if (url.includes('/api/clones?scope=public') && authHeader === 'Bearer token-user-2') {
        return jsonResponse([])
      }

      if (url.includes('/api/clones?scope=friend') && authHeader === 'Bearer token-user-2') {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=mine') && authHeader === 'Bearer token-user-2') {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      if (url.includes('/api/voices?scope=public') && authHeader === 'Bearer token-user-2') {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      if (url.includes('/api/voices?scope=friend') && authHeader === 'Bearer token-user-2') {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      if (url.endsWith('/api/chat/conversations') && authHeader === 'Bearer token-user-2') {
        return jsonResponse([])
      }

      if (url.endsWith('/api/debates') && authHeader === 'Bearer token-user-2') {
        return jsonResponse([])
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
    expect(screen.getByText('내 AI 프로필 자산')).toBeInTheDocument()
    expect(screen.getByText('사용자당 1개만 유지되며, 새로 만들면 기존 자산을 갱신합니다.')).toBeInTheDocument()
    expect(
      screen.getByText('사용자1님의 자산과 친구 관계, 공개 자산을 한 화면에서 관리하며 대화와 논쟁 흐름을 이어갈 수 있습니다.'),
    ).toBeInTheDocument()

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
    expect(screen.getByText('사용자당 1개만 유지되며, 새로 만들면 기존 자산을 갱신합니다.')).toBeInTheDocument()
    expect(
      screen.getByText('사용자2님의 자산과 친구 관계, 공개 자산을 한 화면에서 관리하며 대화와 논쟁 흐름을 이어갈 수 있습니다.'),
    ).toBeInTheDocument()
    expect(fetchSpy).toHaveBeenCalled()
  })

  it('loads the friends tab, searches users, and handles request actions', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'friend-ui-token')

    const fetchSpy = vi.spyOn(window, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine') || url.includes('/api/clones?scope=friend') || url.includes('/api/clones?scope=public') || url.includes('/api/voices?scope=mine') || url.includes('/api/voices?scope=friend') || url.includes('/api/voices?scope=public')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/friends')) {
        if (init?.method === 'DELETE') {
          return jsonResponse({
            friendRequestId: 99,
            status: 'CANCELED',
            createdAt: '2026-03-27T00:00:00.000Z',
            respondedAt: '2026-03-27T01:00:00.000Z',
            requester: { userId: 10, username: 'user10', displayName: '사용자10' },
            receiver: { userId: 77, username: 'friend77', displayName: '친구77' },
          })
        }

        return jsonResponse([
          {
            user: { userId: 77, username: 'friend77', displayName: '친구77' },
            friendsSince: '2026-03-27T01:00:00.000Z',
          },
        ])
      }

      if (url.endsWith('/api/friends/requests/received')) {
        return jsonResponse([
          {
            friendRequestId: 21,
            status: 'PENDING',
            createdAt: '2026-03-27T00:00:00.000Z',
            respondedAt: null,
            requester: { userId: 88, username: 'incoming88', displayName: '받은친구' },
            receiver: { userId: 10, username: 'user10', displayName: '사용자10' },
          },
        ])
      }

      if (url.endsWith('/api/friends/requests/sent')) {
        return jsonResponse([
          {
            friendRequestId: 22,
            status: 'PENDING',
            createdAt: '2026-03-27T00:10:00.000Z',
            respondedAt: null,
            requester: { userId: 10, username: 'user10', displayName: '사용자10' },
            receiver: { userId: 99, username: 'outgoing99', displayName: '보낸친구' },
          },
        ])
      }

      if (url.includes('/api/friends/users/search')) {
        return jsonResponse([
          { userId: 55, username: 'search55', displayName: '검색친구' },
        ])
      }

      if (url.endsWith('/api/friends/requests') && init?.method === 'POST') {
        expect(String(init.body)).toContain('"receiverUserId":55')
        return jsonResponse({
          friendRequestId: 33,
          status: 'PENDING',
          createdAt: '2026-03-27T00:20:00.000Z',
          respondedAt: null,
          requester: { userId: 10, username: 'user10', displayName: '사용자10' },
          receiver: { userId: 55, username: 'search55', displayName: '검색친구' },
        })
      }

      if (url.endsWith('/api/friends/requests/21/accept') && init?.method === 'POST') {
        return jsonResponse({
          friendRequestId: 21,
          status: 'ACCEPTED',
          createdAt: '2026-03-27T00:00:00.000Z',
          respondedAt: '2026-03-27T00:30:00.000Z',
          requester: { userId: 88, username: 'incoming88', displayName: '받은친구' },
          receiver: { userId: 10, username: 'user10', displayName: '사용자10' },
        })
      }

      if (url.endsWith('/api/friends/requests/22/cancel') && init?.method === 'POST') {
        return jsonResponse({
          friendRequestId: 22,
          status: 'CANCELED',
          createdAt: '2026-03-27T00:10:00.000Z',
          respondedAt: '2026-03-27T00:40:00.000Z',
          requester: { userId: 10, username: 'user10', displayName: '사용자10' },
          receiver: { userId: 99, username: 'outgoing99', displayName: '보낸친구' },
        })
      }

      if (url.endsWith('/api/friends/77') && init?.method === 'DELETE') {
        return jsonResponse({
          friendRequestId: 99,
          status: 'CANCELED',
          createdAt: '2026-03-27T00:00:00.000Z',
          respondedAt: '2026-03-27T01:10:00.000Z',
          requester: { userId: 10, username: 'user10', displayName: '사용자10' },
          receiver: { userId: 77, username: 'friend77', displayName: '친구77' },
        })
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 10, username: 'user10', displayName: '사용자10' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    await user.click(await screen.findByRole('button', { name: '친구' }))

    expect(await screen.findByRole('heading', { name: '사용자 검색' })).toBeInTheDocument()
    expect(await screen.findByText('친구77')).toBeInTheDocument()
    expect(screen.getByText('받은친구')).toBeInTheDocument()
    expect(screen.getByText('보낸친구')).toBeInTheDocument()

    await user.type(screen.getByLabelText('표시명 또는 아이디'), '검색')
    await user.click(screen.getByRole('button', { name: '사용자 검색' }))
    expect(await screen.findByText('검색친구')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '친구 요청' }))
    await user.click(screen.getByRole('button', { name: '수락' }))
    await user.click(screen.getByRole('button', { name: '요청 취소' }))
    await user.click(screen.getByRole('button', { name: '친구 해제' }))

    expect(fetchSpy).toHaveBeenCalled()
  })

  it('wires the account bar logout and deactivate buttons', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'fetch').mockImplementation(async (input) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return new Response(JSON.stringify([{ question: 'Q1', choices: ['A', 'B'] }]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      if (url.includes('/api/clones?scope=mine') || url.includes('/api/clones?scope=friend') || url.includes('/api/clones?scope=public') || url.includes('/api/voices?scope=mine') || url.includes('/api/voices?scope=friend') || url.includes('/api/voices?scope=public')) {
        return new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    const onLogout = vi.fn()
    const onDeactivate = vi.fn(async () => {})

    render(
      <CloneStudioPage
        currentUser={{ userId: 3, username: 'user3', displayName: '사용자3' }}
        deactivating={false}
        onDeactivate={onDeactivate}
        onLogout={onLogout}
      />,
    )

    await user.click(await screen.findByRole('button', { name: '로그아웃' }))
    await user.click(screen.getByRole('button', { name: '회원 탈퇴' }))

    expect(onLogout).toHaveBeenCalledTimes(1)
    expect(onDeactivate).toHaveBeenCalledTimes(1)
  })

  it('renders the new SNS shell scaffold and keeps the legacy studio inside Profile', async () => {
    const user = userEvent.setup()
    vi.spyOn(window, 'fetch').mockImplementation(async (input) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine') || url.includes('/api/clones?scope=friend') || url.includes('/api/clones?scope=public')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=mine') || url.includes('/api/voices?scope=friend') || url.includes('/api/voices?scope=public')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/posts/feed')) {
        return jsonResponse([])
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 14, username: 'user14', displayName: '사용자14' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    expect(await screen.findByRole('button', { name: 'Profile' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('기존 스튜디오를 유지하면서 계정 공개성을 먼저 고정합니다.')).toBeInTheDocument()
    expect(screen.getByText('사용자14님의 자산과 친구 관계, 공개 자산을 한 화면에서 관리하며 대화와 논쟁 흐름을 이어갈 수 있습니다.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Home' }))
    expect(screen.getByRole('button', { name: 'Home' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('피드와 게시물 경험을 위한 자리입니다.')).toBeInTheDocument()
    expect(await screen.findByText('새 게시물')).toBeInTheDocument()
    expect(screen.getByText('피드에 표시할 게시물이 아직 없습니다.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Profile' }))
    expect(screen.getByRole('button', { name: 'Profile' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText('사용자14님의 자산과 친구 관계, 공개 자산을 한 화면에서 관리하며 대화와 논쟁 흐름을 이어갈 수 있습니다.')).toBeInTheDocument()
  })

  it('searches public accounts and updates account visibility from the profile shell', async () => {
    const user = userEvent.setup()

    vi.spyOn(window, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine') || url.includes('/api/clones?scope=friend') || url.includes('/api/clones?scope=public')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=mine') || url.includes('/api/voices?scope=friend') || url.includes('/api/voices?scope=public')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      if (url.includes('/api/follows/users/search')) {
        return jsonResponse([
          {
            userId: 81,
            username: 'public81',
            displayName: '공개 사용자',
            visibility: 'PUBLIC',
            following: false,
          },
          {
            userId: 82,
            username: 'private82',
            displayName: '이미 팔로우 중인 비공개 사용자',
            visibility: 'PRIVATE',
            following: true,
          },
        ])
      }

      if (url.endsWith('/api/follows/81') && init?.method === 'POST') {
        return jsonResponse({
          userId: 81,
          username: 'public81',
          displayName: '공개 사용자',
          visibility: 'PUBLIC',
          following: true,
        })
      }

      if (url.endsWith('/api/profiles/me/visibility') && init?.method === 'PATCH') {
        expect(String(init.body)).toContain('"visibility":"PRIVATE"')
        return jsonResponse({
          userId: 14,
          username: 'user14',
          displayName: '사용자14',
          visibility: 'PRIVATE',
          me: true,
          following: false,
        })
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 14, username: 'user14', displayName: '사용자14', visibility: 'PUBLIC' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    expect(await screen.findByRole('button', { name: '공개 계정' })).toHaveClass('sns-visibility-button-active')

    await user.click(screen.getByRole('button', { name: '비공개 계정' }))
    expect(await screen.findByRole('button', { name: '비공개 계정' })).toHaveClass('sns-visibility-button-active')

    await user.click(screen.getByRole('button', { name: 'Search' }))
    await user.type(screen.getByPlaceholderText('표시명 또는 아이디를 입력하세요'), '사용자')
    await user.click(screen.getByRole('button', { name: '검색' }))

    expect(await screen.findByText('공개 사용자')).toBeInTheDocument()
    expect(screen.getByText('이미 팔로우 중인 비공개 사용자')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '언팔로우' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '팔로우' }))
    expect((await screen.findAllByRole('button', { name: '언팔로우' })).length).toBe(2)
  })

  it('creates a post in Home and loads my posts from Profile', async () => {
    const user = userEvent.setup()

    vi.spyOn(window, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine') || url.includes('/api/clones?scope=friend') || url.includes('/api/clones?scope=public')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=mine') || url.includes('/api/voices?scope=friend') || url.includes('/api/voices?scope=public')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/posts/feed')) {
        return jsonResponse([
          {
            postId: 91,
            ownerUserId: 40,
            ownerUsername: 'miso40',
            ownerDisplayName: '미소40',
            ownerVisibility: 'PUBLIC',
            content: '피드에서 먼저 보이는 게시물',
            createdAt: '2026-03-28T00:00:00.000Z',
          },
        ])
      }

      if (url.endsWith('/api/posts') && init?.method === 'POST') {
        expect(String(init.body)).toContain('새로 추가한 게시물')
        return jsonResponse({
          postId: 92,
          ownerUserId: 15,
          ownerUsername: 'user15',
          ownerDisplayName: '사용자15',
          ownerVisibility: 'PUBLIC',
          content: '새로 추가한 게시물',
          createdAt: '2026-03-28T00:10:00.000Z',
        })
      }

      if (url.endsWith('/api/profiles/me/posts')) {
        return jsonResponse([
          {
            postId: 101,
            ownerUserId: 15,
            ownerUsername: 'user15',
            ownerDisplayName: '사용자15',
            ownerVisibility: 'PUBLIC',
            content: '프로필에서 불러온 내 게시물',
            createdAt: '2026-03-28T00:20:00.000Z',
          },
        ])
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 15, username: 'user15', displayName: '사용자15', visibility: 'PUBLIC' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    await user.click(await screen.findByRole('button', { name: 'Home' }))
    expect(await screen.findByText('피드에서 먼저 보이는 게시물')).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText('오늘 공유하고 싶은 생각을 적어보세요'), '새로 추가한 게시물')
    await user.click(screen.getByRole('button', { name: '게시하기' }))
    expect(await screen.findByText('새로 추가한 게시물')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Profile' }))
    await user.click(screen.getByRole('button', { name: '내 게시물 불러오기' }))
    expect(await screen.findByText('프로필에서 불러온 내 게시물')).toBeInTheDocument()
  })

  it('starts a human DM from Search and sends a message in the DM tab', async () => {
    const user = userEvent.setup()

    vi.spyOn(window, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine') || url.includes('/api/clones?scope=friend') || url.includes('/api/clones?scope=public')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=mine') || url.includes('/api/voices?scope=friend') || url.includes('/api/voices?scope=public')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      if (url.includes('/api/follows/users/search')) {
        return jsonResponse([
          {
            userId: 61,
            username: 'public61',
            displayName: '대화 상대',
            visibility: 'PUBLIC',
            following: false,
          },
        ])
      }

      if (url.endsWith('/api/dms/threads') && init?.method === 'POST') {
        expect(String(init.body)).toContain('"targetUserId":61')
        return jsonResponse({
          threadId: 301,
          otherParticipant: {
            userId: 61,
            username: 'public61',
            displayName: '대화 상대',
            visibility: 'PUBLIC',
          },
          createdAt: '2026-03-28T00:00:00.000Z',
          messages: [],
        })
      }

      if (url.endsWith('/api/dms/threads')) {
        return jsonResponse([
          {
            threadId: 301,
            otherParticipant: {
              userId: 61,
              username: 'public61',
              displayName: '대화 상대',
              visibility: 'PUBLIC',
            },
            createdAt: '2026-03-28T00:00:00.000Z',
            latestMessagePreview: '',
            latestMessageCreatedAt: null,
          },
        ])
      }

      if (url.endsWith('/api/dms/threads/301/messages') && init?.method === 'POST') {
        expect(String(init.body)).toContain('안녕하세요, 처음 메시지예요')
        return jsonResponse({
          messageId: 401,
          senderUserId: 16,
          senderDisplayName: '사용자16',
          content: '안녕하세요, 처음 메시지예요',
          createdAt: '2026-03-28T00:01:00.000Z',
        })
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 16, username: 'user16', displayName: '사용자16', visibility: 'PUBLIC' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    await user.click(await screen.findByRole('button', { name: 'Search' }))
    await user.type(screen.getByPlaceholderText('표시명 또는 아이디를 입력하세요'), '대화')
    await user.click(screen.getByRole('button', { name: '검색' }))
    expect(await screen.findByText('대화 상대')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'DM 시작' }))
    expect(await screen.findByRole('button', { name: 'DM' })).toHaveAttribute('aria-pressed', 'true')
    expect(await screen.findAllByText('대화 상대')).toHaveLength(2)
    expect(screen.getByText('아직 메시지가 없습니다. 첫 메시지를 보내보세요.')).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText('메시지를 입력하세요'), '안녕하세요, 처음 메시지예요')
    await user.click(screen.getByRole('button', { name: '보내기' }))
    expect(await screen.findByText('안녕하세요, 처음 메시지예요')).toBeInTheDocument()
  })

  it('streams a chat reply through the live session flow', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'chat-token')

    vi.spyOn(window, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine')) {
        return jsonResponse([
          {
            cloneId: 101,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: 'Alpha',
            shortDescription: '대화를 시작할 클론',
            isPublic: false,
            ownerDisplayName: '사용자4',
          },
        ])
      }

      if (url.includes('/api/clones?scope=public')) {
        return jsonResponse([])
      }

      if (url.includes('/api/clones?scope=friend')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=mine')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=public')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=friend')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/conversations')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/messages/stream')) {
        const headers = new Headers(init?.headers)
        expect(headers.get('Authorization')).toBe('Bearer chat-token')
        return ndjsonResponse([
          { type: 'message', conversationId: 77, assistantMessage: '안녕하세요. 오늘은 이렇게 정리해볼게요.' },
          { type: 'audio_chunk', audioFormat: 'pcm_s16le', sampleRate: 24000, channels: 1, chunkBase64: 'AAAB' },
          { type: 'done', conversationId: 77, ttsVoiceId: 'voice-1', hasAudio: true },
        ])
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 4, username: 'user4', displayName: '사용자4' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    const cloneTitle = await screen.findByText('Alpha', { selector: 'h2' })
    const cloneCard = cloneTitle.closest('button')
    if (!cloneCard) {
      throw new Error('Clone card button not found.')
    }

    await user.click(cloneCard)
    await user.click(screen.getByRole('button', { name: /나와 대화하기/ }))
    await user.click(screen.getByRole('button', { name: /대화 시작/ }))

    await user.type(await screen.findByLabelText('메시지'), '오늘 일정 정리해줘')
    await user.click(screen.getByRole('button', { name: /보내기/ }))

    expect(await screen.findByText('오늘 일정 정리해줘')).toBeInTheDocument()
    expect(await screen.findByText('안녕하세요. 오늘은 이렇게 정리해볼게요.')).toBeInTheDocument()
    expect(playerSpies.configure).toHaveBeenCalledWith(24000, 1)
    expect(playerSpies.appendBase64Chunk).toHaveBeenCalledWith('AAAB')
    expect(playerSpies.finish).toHaveBeenCalled()
  })

  it('starts a debate stream and exits cleanly while waiting for the next turn', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'debate-token')

    vi.spyOn(window, 'fetch').mockImplementation((input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return Promise.resolve(jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }]))
      }

      if (url.includes('/api/clones?scope=mine')) {
        return Promise.resolve(jsonResponse([
          {
            cloneId: 101,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: 'Alpha',
            shortDescription: '첫 번째 토론 클론',
            isPublic: false,
            ownerDisplayName: '사용자5',
          },
          {
            cloneId: 202,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: 'Beta',
            shortDescription: '두 번째 토론 클론',
            isPublic: false,
            ownerDisplayName: '사용자5',
          },
        ]))
      }

      if (url.includes('/api/clones?scope=public')) {
        return Promise.resolve(jsonResponse([]))
      }

      if (url.includes('/api/clones?scope=friend')) {
        return Promise.resolve(jsonResponse([]))
      }

      if (url.includes('/api/voices?scope=mine')) {
        return Promise.resolve(jsonResponse([
          {
            registeredVoiceId: 1,
            voiceId: 'voice-a',
            displayName: '목소리 A',
            preferredName: 'voicea',
            originalFilename: 'a.wav',
            audioMimeType: 'audio/wav',
            isPublic: false,
            ownerDisplayName: '사용자5',
          },
          {
            registeredVoiceId: 2,
            voiceId: 'voice-b',
            displayName: '목소리 B',
            preferredName: 'voiceb',
            originalFilename: 'b.wav',
            audioMimeType: 'audio/wav',
            isPublic: false,
            ownerDisplayName: '사용자5',
          },
        ]))
      }

      if (url.includes('/api/voices?scope=public')) {
        return Promise.resolve(jsonResponse([]))
      }

      if (url.includes('/api/voices?scope=friend')) {
        return Promise.resolve(jsonResponse([]))
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return Promise.resolve(jsonResponse([]))
      }

      if (url.endsWith('/api/debates/stream')) {
        const headers = new Headers(init?.headers)
        expect(headers.get('Authorization')).toBe('Bearer debate-token')
        return Promise.resolve(ndjsonResponse([
          {
            type: 'turn',
            debateSessionId: 55,
            topic: '원격근무가 더 효율적인가?',
            turn: { turnIndex: 1, speaker: 'CLONE_A', cloneId: 101, content: '저는 원격근무가 더 효율적이라고 봅니다.' },
          },
          { type: 'audio_chunk', audioFormat: 'pcm_s16le', sampleRate: 24000, channels: 1, chunkBase64: 'AAAB' },
          { type: 'done', debateSessionId: 55, turnIndex: 1, ttsVoiceId: 'voice-a', hasAudio: true },
        ]))
      }

      if (url.endsWith('/api/debates/55/next/stream')) {
        const signal = init?.signal

        return new Promise<Response>((_resolve, reject) => {
          signal?.addEventListener('abort', () => {
            reject(new DOMException('Aborted', 'AbortError'))
          }, { once: true })
        })
      }

      return Promise.reject(new Error(`Unhandled request: ${url}`))
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 5, username: 'user5', displayName: '사용자5' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    const firstCloneTitle = await screen.findByText('Alpha', { selector: 'h2' })
    const firstCloneCard = firstCloneTitle.closest('button')
    if (!firstCloneCard) {
      throw new Error('Clone card button not found.')
    }

    await user.click(firstCloneCard)
    await user.click(screen.getByRole('button', { name: /논쟁시키기/ }))
    await user.selectOptions(screen.getByLabelText('상대 클론'), '202')
    await user.selectOptions(screen.getByLabelText('Alpha의 목소리'), '1')
    await user.selectOptions(screen.getByLabelText('Beta의 목소리'), '2')
    await user.type(screen.getByLabelText('논쟁 주제'), '원격근무가 더 효율적인가?')
    await user.click(screen.getByRole('button', { name: /논쟁 시작/ }))

    expect(await screen.findByText('저는 원격근무가 더 효율적이라고 봅니다.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '종료' }))

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '클론 스튜디오' })).toHaveClass('tab-chip-active')
    })
    expect(playerSpies.dispose).toHaveBeenCalled()
  })

  it('opens previously saved chat and debate history from the live tab', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'history-token')

    vi.spyOn(window, 'fetch').mockImplementation(async (input) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine')) {
        return jsonResponse([
          {
            cloneId: 101,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: 'Alpha',
            shortDescription: '첫 번째 클론',
            isPublic: false,
            ownerDisplayName: '사용자6',
          },
          {
            cloneId: 202,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: 'Beta',
            shortDescription: '두 번째 클론',
            isPublic: false,
            ownerDisplayName: '사용자6',
          },
        ])
      }

      if (url.includes('/api/clones?scope=public')) {
        return jsonResponse([])
      }

      if (url.includes('/api/clones?scope=friend')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=mine')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=public')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=friend')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/conversations')) {
        return jsonResponse([
          {
            conversationId: 77,
            cloneId: 101,
            cloneAlias: 'Alpha',
            createdAt: '2026-03-26T01:00:00.000Z',
            latestMessagePreview: '지난번 채팅 마지막 메시지',
            messageCount: 2,
          },
        ])
      }

      if (url.endsWith('/api/debates')) {
        return jsonResponse([
          {
            debateSessionId: 55,
            cloneAId: 101,
            cloneAAlias: 'Alpha',
            cloneBId: 202,
            cloneBAlias: 'Beta',
            topic: '원격근무가 더 효율적인가?',
            createdAt: '2026-03-26T02:00:00.000Z',
            turnCount: 2,
          },
        ])
      }

      if (url.endsWith('/api/chat/conversations/77')) {
        return jsonResponse({
          conversationId: 77,
          cloneId: 101,
          cloneAlias: 'Alpha',
          cloneShortDescription: '첫 번째 클론',
          createdAt: '2026-03-26T01:00:00.000Z',
          messages: [
            { role: 'user', content: '지난 질문', createdAt: '2026-03-26T01:00:01.000Z', ttsAudioUrl: null, ttsVoiceId: null },
            { role: 'assistant', content: '지난 답변', createdAt: '2026-03-26T01:00:02.000Z', ttsAudioUrl: 'https://cdn.example/chat.mp3', ttsVoiceId: 'voice-chat' },
          ],
        })
      }

      if (url.endsWith('/api/debates/55')) {
        return jsonResponse({
          debateSessionId: 55,
          cloneAId: 101,
          cloneAAlias: 'Alpha',
          cloneAShortDescription: '첫 번째 클론',
          cloneAVoiceId: 1,
          cloneBId: 202,
          cloneBAlias: 'Beta',
          cloneBShortDescription: '두 번째 클론',
          cloneBVoiceId: 2,
          topic: '원격근무가 더 효율적인가?',
          createdAt: '2026-03-26T02:00:00.000Z',
          turns: [
            {
              turnIndex: 1,
              speaker: 'CLONE_A',
              cloneId: 101,
              content: '저는 원격근무가 더 효율적이라고 봅니다.',
              createdAt: '2026-03-26T02:00:01.000Z',
              ttsAudioUrl: null,
              ttsVoiceId: null,
            },
            {
              turnIndex: 2,
              speaker: 'CLONE_B',
              cloneId: 202,
              content: '저는 대면 협업이 더 효율적이라고 봅니다.',
              createdAt: '2026-03-26T02:00:02.000Z',
              ttsAudioUrl: 'https://cdn.example/debate.mp3',
              ttsVoiceId: 'voice-debate',
            },
          ],
        })
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 6, username: 'user6', displayName: '사용자6' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    await user.click(await screen.findByRole('button', { name: '라이브 세션' }))
    await user.click(await screen.findByRole('button', { name: /지난번 채팅 마지막 메시지/ }))

    expect(await screen.findByText('지난 질문')).toBeInTheDocument()
    expect(await screen.findByText('지난 답변')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /원격근무가 더 효율적인가/ }))

    expect(await screen.findByText('저는 원격근무가 더 효율적이라고 봅니다.')).toBeInTheDocument()
    expect(await screen.findByText('저는 대면 협업이 더 효율적이라고 봅니다.')).toBeInTheDocument()
  })

  it('separates mine and public assets and only shows visibility toggles for owned assets', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'visibility-token')

    vi.spyOn(window, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine')) {
        return jsonResponse([
          {
            cloneId: 101,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: '내 클론',
            shortDescription: '내가 만든 클론',
            isPublic: false,
            ownerDisplayName: '사용자7',
          },
        ])
      }

      if (url.includes('/api/clones?scope=public')) {
        return jsonResponse([
          {
            cloneId: 202,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: '공개 클론',
            shortDescription: '다른 사용자의 공개 클론',
            isPublic: true,
            ownerDisplayName: '다른 사용자',
          },
        ])
      }

      if (url.includes('/api/clones?scope=friend')) {
        return jsonResponse([
          {
            cloneId: 303,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: '친구 클론',
            shortDescription: '친구가 공유한 비공개 클론',
            isPublic: false,
            ownerDisplayName: '친구 사용자',
          },
        ])
      }

      if (url.includes('/api/voices?scope=mine')) {
        return jsonResponse([
          {
            registeredVoiceId: 1,
            voiceId: 'voice-a',
            displayName: '내 목소리',
            preferredName: 'voicea',
            originalFilename: 'a.wav',
            audioMimeType: 'audio/wav',
            isPublic: false,
            ownerDisplayName: '사용자7',
          },
        ])
      }

      if (url.includes('/api/voices?scope=public')) {
        return jsonResponse([
          {
            registeredVoiceId: 2,
            voiceId: 'voice-b',
            displayName: '공개 목소리',
            preferredName: 'voiceb',
            originalFilename: 'b.wav',
            audioMimeType: 'audio/wav',
            isPublic: true,
            ownerDisplayName: '다른 사용자',
          },
        ])
      }

      if (url.includes('/api/voices?scope=friend')) {
        return jsonResponse([
          {
            registeredVoiceId: 3,
            voiceId: 'voice-friend',
            displayName: '친구 목소리',
            preferredName: 'voicefriend',
            originalFilename: 'friend.wav',
            audioMimeType: 'audio/wav',
            isPublic: false,
            ownerDisplayName: '친구 사용자',
          },
        ])
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/clones/101/visibility')) {
        expect(init?.method).toBe('PATCH')
        return jsonResponse({ cloneId: 101, isPublic: true })
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 7, username: 'user7', displayName: '사용자7' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    expect((await screen.findAllByText('내 클론', { selector: 'h2' })).length).toBeGreaterThan(0)
    expect(screen.getAllByText('친구 클론', { selector: 'h2' }).length).toBeGreaterThan(0)
    expect(screen.getAllByText('공개 클론', { selector: 'h2' }).length).toBeGreaterThan(0)
    expect(screen.getByText('작성자 다른 사용자')).toBeInTheDocument()
    expect(screen.getByText('작성자 친구 사용자')).toBeInTheDocument()

    const mineCloneButton = screen
      .getAllByText('내가 만든 클론')
      .map((element) => element.closest('button'))
      .find((element): element is HTMLButtonElement => element instanceof HTMLButtonElement)
    if (!mineCloneButton) {
      throw new Error('Owned clone card button not found.')
    }

    await user.click(mineCloneButton)
    expect(await screen.findByRole('button', { name: '공개로 전환' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: '공개로 전환' }))
    expect(await screen.findByRole('button', { name: '비공개로 전환' })).toBeInTheDocument()
  })

  it('starts chat with a public clone and public voice without showing owner-only visibility actions', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'public-chat-token')

    vi.spyOn(window, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine')) {
        return jsonResponse([])
      }

      if (url.includes('/api/clones?scope=friend')) {
        return jsonResponse([])
      }

      if (url.includes('/api/clones?scope=public')) {
        return jsonResponse([
          {
            cloneId: 303,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: '공개 화자',
            shortDescription: '외부 사용자가 공개한 클론',
            isPublic: true,
            ownerDisplayName: '공개 작성자',
          },
        ])
      }

      if (url.includes('/api/voices?scope=mine')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=friend')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=public')) {
        return jsonResponse([
          {
            registeredVoiceId: 9,
            voiceId: 'voice-public',
            displayName: '공개 음성',
            preferredName: 'publicvoice',
            originalFilename: 'public.wav',
            audioMimeType: 'audio/wav',
            isPublic: true,
            ownerDisplayName: '공개 작성자',
          },
        ])
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/messages/stream')) {
        expect(init?.method).toBe('POST')
        expect(init?.body).toContain('"promptGenerationLogId":303')
        expect(init?.body).toContain('"registeredVoiceId":9')
        return ndjsonResponse([
          { type: 'message', conversationId: 88, assistantMessage: '공개 자산으로도 대화를 시작할 수 있습니다.' },
          { type: 'done', conversationId: 88, ttsVoiceId: 'voice-public', hasAudio: false },
        ])
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 8, username: 'user8', displayName: '사용자8' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    const publicCloneCard = (await screen.findByText('외부 사용자가 공개한 클론')).closest('button')
    if (!publicCloneCard) {
      throw new Error('Public clone card button not found.')
    }

    await user.click(publicCloneCard)
    expect(screen.getByText('공개 작성자님이 공개한 클론입니다. 지금 계정에서도 바로 대화와 논쟁에 사용할 수 있습니다.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '공개로 전환' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /나와 대화하기/ }))
    expect(screen.getByText('내 음성은 여기서 바로 공개 전환할 수 있고, 친구 음성과 공개 음성은 작성자 표기를 확인한 뒤 현재 계정에서도 바로 사용할 수 있습니다.')).toBeInTheDocument()
    expect(screen.getAllByText('작성자 공개 작성자').length).toBeGreaterThan(0)

    const publicVoiceCard = screen.getByText('공개 음성 · public.wav').closest('label')
    if (!publicVoiceCard) {
      throw new Error('Public voice card not found.')
    }
    await user.click(publicVoiceCard)
    await user.click(screen.getByRole('button', { name: /대화 시작/ }))
    await user.type(await screen.findByLabelText('메시지'), '공개 자산 테스트')
    await user.click(screen.getByRole('button', { name: /보내기/ }))

    expect(await screen.findByText('공개 자산으로도 대화를 시작할 수 있습니다.')).toBeInTheDocument()
  })

  it('shows a friendly message when a public asset becomes unavailable before chat starts', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'public-chat-error-token')

    vi.spyOn(window, 'fetch').mockImplementation(async (input) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine')) {
        return jsonResponse([])
      }

      if (url.includes('/api/clones?scope=friend')) {
        return jsonResponse([])
      }

      if (url.includes('/api/clones?scope=public')) {
        return jsonResponse([
          {
            cloneId: 404,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: '잠긴 공개 클론',
            shortDescription: '방금 비공개로 전환된 클론',
            isPublic: true,
            ownerDisplayName: '작성자9',
          },
        ])
      }

      if (url.includes('/api/voices?scope=mine') || url.includes('/api/voices?scope=friend') || url.includes('/api/voices?scope=public')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/messages/stream')) {
        return new Response(JSON.stringify({ message: 'Forbidden' }), {
          status: 403,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 9, username: 'user9', displayName: '사용자9' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    const publicCloneCard = (await screen.findByText('방금 비공개로 전환된 클론')).closest('button')
    if (!publicCloneCard) {
      throw new Error('Unavailable public clone card button not found.')
    }

    await user.click(publicCloneCard)
    await user.click(screen.getByRole('button', { name: /나와 대화하기/ }))
    await user.click(screen.getByRole('button', { name: /대화 시작/ }))
    await user.type(await screen.findByLabelText('메시지'), '시작해볼까?')
    await user.click(screen.getByRole('button', { name: /보내기/ }))

    expect(await screen.findByText(/선택한 클론 또는 목소리로 대화를 시작할 수 없습니다/)).toBeInTheDocument()
    expect(screen.getByText(/다른 사용자가 자산을 비공개로 전환했거나 현재 계정으로 접근할 수 없습니다/)).toBeInTheDocument()
  })

  it('shows a friendly message when a friend asset becomes unavailable before chat starts', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'friend-chat-error-token')

    vi.spyOn(window, 'fetch').mockImplementation(async (input) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine')) {
        return jsonResponse([])
      }

      if (url.includes('/api/clones?scope=friend')) {
        return jsonResponse([
          {
            cloneId: 505,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: '친구 비공개 클론',
            shortDescription: '친구 해제 또는 권한 상실 직전 상태',
            isPublic: false,
            ownerDisplayName: '친구 작성자',
          },
        ])
      }

      if (url.includes('/api/clones?scope=public')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=mine')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=friend')) {
        return jsonResponse([
          {
            registeredVoiceId: 15,
            voiceId: 'friend-voice-locked',
            displayName: '친구 전용 음성',
            preferredName: 'friendvoice',
            originalFilename: 'friend.wav',
            audioMimeType: 'audio/wav',
            isPublic: false,
            ownerDisplayName: '친구 작성자',
          },
        ])
      }

      if (url.includes('/api/voices?scope=public')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/messages/stream')) {
        return new Response(JSON.stringify({ message: 'Forbidden' }), {
          status: 403,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 13, username: 'user13', displayName: '사용자13' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    const friendCloneCard = (await screen.findByText('친구 해제 또는 권한 상실 직전 상태')).closest('button')
    if (!friendCloneCard) {
      throw new Error('Unavailable friend clone card button not found.')
    }

    await user.click(friendCloneCard)
    await user.click(screen.getByRole('button', { name: /나와 대화하기/ }))
    const friendVoiceCard = screen.getByText('친구 전용 음성 · friend.wav').closest('label')
    if (!friendVoiceCard) {
      throw new Error('Unavailable friend voice card not found.')
    }
    await user.click(friendVoiceCard)
    await user.click(screen.getByRole('button', { name: /대화 시작/ }))
    await user.type(await screen.findByLabelText('메시지'), '권한이 아직 있나 확인해볼게')
    await user.click(screen.getByRole('button', { name: /보내기/ }))

    expect(await screen.findByText(/선택한 클론 또는 목소리로 대화를 시작할 수 없습니다/)).toBeInTheDocument()
    expect(screen.getByText(/다른 사용자가 자산을 비공개로 전환했거나 현재 계정으로 접근할 수 없습니다/)).toBeInTheDocument()
  })

  it('starts chat with a friend clone and friend voice without showing visibility controls', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'friend-chat-token')

    vi.spyOn(window, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.includes('/api/clones?scope=mine')) {
        return jsonResponse([])
      }

      if (url.includes('/api/clones?scope=friend')) {
        return jsonResponse([
          {
            cloneId: 505,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: '친구 화자',
            shortDescription: '친구가 공유한 비공개 클론',
            isPublic: false,
            ownerDisplayName: '친구 작성자',
          },
        ])
      }

      if (url.includes('/api/clones?scope=public')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=mine')) {
        return jsonResponse([])
      }

      if (url.includes('/api/voices?scope=friend')) {
        return jsonResponse([
          {
            registeredVoiceId: 15,
            voiceId: 'voice-friend-private',
            displayName: '친구 전용 음성',
            preferredName: 'friendprivate',
            originalFilename: 'friend-private.wav',
            audioMimeType: 'audio/wav',
            isPublic: false,
            ownerDisplayName: '친구 작성자',
          },
        ])
      }

      if (url.includes('/api/voices?scope=public')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return jsonResponse([])
      }

      if (url.endsWith('/api/chat/messages/stream')) {
        expect(init?.method).toBe('POST')
        expect(String(init?.body)).toContain('"promptGenerationLogId":505')
        expect(String(init?.body)).toContain('"registeredVoiceId":15')
        return ndjsonResponse([
          { type: 'message', conversationId: 123, assistantMessage: '친구 자산으로도 대화를 시작할 수 있어요.' },
          { type: 'done', conversationId: 123, ttsVoiceId: 'voice-friend-private', hasAudio: false },
        ])
      }

      throw new Error(`Unhandled request: ${url}`)
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 11, username: 'user11', displayName: '사용자11' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    const friendCloneCard = (await screen.findByText('친구가 공유한 비공개 클론')).closest('button')
    if (!friendCloneCard) {
      throw new Error('Friend clone card button not found.')
    }

    await user.click(friendCloneCard)
    expect(screen.getAllByText('작성자 친구 작성자').length).toBeGreaterThan(0)
    expect(screen.queryByRole('button', { name: '공개로 전환' })).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /나와 대화하기/ }))
    expect(screen.getByText('친구 전용 음성 · friend-private.wav')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '비공개로 전환' })).not.toBeInTheDocument()

    const friendVoiceCard = screen.getByText('친구 전용 음성 · friend-private.wav').closest('label')
    if (!friendVoiceCard) {
      throw new Error('Friend voice card not found.')
    }

    await user.click(friendVoiceCard)
    await user.click(screen.getByRole('button', { name: /대화 시작/ }))
    await user.type(await screen.findByLabelText('메시지'), '친구 자산으로 이야기해보자')
    await user.click(screen.getByRole('button', { name: /보내기/ }))

    expect(await screen.findByText('친구 자산으로도 대화를 시작할 수 있어요.')).toBeInTheDocument()
  })

  it('starts a debate with friend clones and friend voices', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'friend-debate-token')

    vi.spyOn(window, 'fetch').mockImplementation((input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return Promise.resolve(jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }]))
      }

      if (url.includes('/api/clones?scope=mine')) {
        return Promise.resolve(jsonResponse([]))
      }

      if (url.includes('/api/clones?scope=friend')) {
        return Promise.resolve(jsonResponse([
          {
            cloneId: 601,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: '친구 A',
            shortDescription: '친구의 첫 번째 비공개 클론',
            isPublic: false,
            ownerDisplayName: '친구 작성자',
          },
          {
            cloneId: 602,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: '친구 B',
            shortDescription: '친구의 두 번째 비공개 클론',
            isPublic: false,
            ownerDisplayName: '친구 작성자',
          },
        ]))
      }

      if (url.includes('/api/clones?scope=public')) {
        return Promise.resolve(jsonResponse([]))
      }

      if (url.includes('/api/voices?scope=mine')) {
        return Promise.resolve(jsonResponse([]))
      }

      if (url.includes('/api/voices?scope=friend')) {
        return Promise.resolve(jsonResponse([
          {
            registeredVoiceId: 31,
            voiceId: 'friend-voice-a',
            displayName: '친구 음성 A',
            preferredName: 'friendvoicea',
            originalFilename: 'friend-a.wav',
            audioMimeType: 'audio/wav',
            isPublic: false,
            ownerDisplayName: '친구 작성자',
          },
          {
            registeredVoiceId: 32,
            voiceId: 'friend-voice-b',
            displayName: '친구 음성 B',
            preferredName: 'friendvoiceb',
            originalFilename: 'friend-b.wav',
            audioMimeType: 'audio/wav',
            isPublic: false,
            ownerDisplayName: '친구 작성자',
          },
        ]))
      }

      if (url.includes('/api/voices?scope=public')) {
        return Promise.resolve(jsonResponse([]))
      }

      if (url.endsWith('/api/chat/conversations') || url.endsWith('/api/debates')) {
        return Promise.resolve(jsonResponse([]))
      }

      if (url.endsWith('/api/debates/stream')) {
        expect(init?.method).toBe('POST')
        expect(String(init?.body)).toContain('"cloneAId":601')
        expect(String(init?.body)).toContain('"cloneBId":602')
        expect(String(init?.body)).toContain('"cloneAVoiceId":31')
        expect(String(init?.body)).toContain('"cloneBVoiceId":32')
        return Promise.resolve(ndjsonResponse([
          {
            type: 'turn',
            debateSessionId: 71,
            topic: '친구 자산으로도 논쟁을 만들 수 있는가?',
            turn: { turnIndex: 1, speaker: 'CLONE_A', cloneId: 601, content: '친구 자산 조합으로도 논쟁을 시작할 수 있습니다.' },
          },
          { type: 'done', debateSessionId: 71, turnIndex: 1, ttsVoiceId: 'friend-voice-a', hasAudio: false },
        ]))
      }

      if (url.endsWith('/api/debates/71/next/stream')) {
        const signal = init?.signal
        return new Promise<Response>((_resolve, reject) => {
          signal?.addEventListener(
            'abort',
            () => {
              reject(new DOMException('Aborted', 'AbortError'))
            },
            { once: true },
          )
        })
      }

      return Promise.reject(new Error(`Unhandled request: ${url}`))
    })

    render(
      <CloneStudioPage
        currentUser={{ userId: 12, username: 'user12', displayName: '사용자12' }}
        deactivating={false}
        onDeactivate={async () => {}}
        onLogout={() => {}}
      />,
    )

    const firstFriendCloneCard = (await screen.findByText('친구의 첫 번째 비공개 클론')).closest('button')
    if (!firstFriendCloneCard) {
      throw new Error('Friend debate clone card button not found.')
    }

    await user.click(firstFriendCloneCard)
    await user.click(screen.getByRole('button', { name: /논쟁시키기/ }))
    await user.selectOptions(screen.getByLabelText('상대 클론'), '602')
    await user.selectOptions(screen.getByLabelText('친구 A의 목소리'), '31')
    await user.selectOptions(screen.getByLabelText('친구 B의 목소리'), '32')
    await user.type(screen.getByLabelText('논쟁 주제'), '친구 자산으로도 논쟁을 만들 수 있는가?')
    await user.click(screen.getByRole('button', { name: /논쟁 시작/ }))

    expect(await screen.findByText('친구 자산 조합으로도 논쟁을 시작할 수 있습니다.')).toBeInTheDocument()
  })
})

function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  })
}

function ndjsonResponse(events: unknown[]) {
  const encoder = new TextEncoder()
  const payload = events.map((event) => JSON.stringify(event)).join('\n')

  return new Response(
    new ReadableStream({
      start(controller) {
        controller.enqueue(encoder.encode(payload))
        controller.close()
      },
    }),
    {
      status: 200,
      headers: { 'Content-Type': 'application/x-ndjson' },
    },
  )
}
