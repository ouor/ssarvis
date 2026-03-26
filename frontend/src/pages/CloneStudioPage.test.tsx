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

      if (url.endsWith('/api/chat/conversations') && authHeader === 'Bearer token-user-1') {
        return jsonResponse([])
      }

      if (url.endsWith('/api/debates') && authHeader === 'Bearer token-user-1') {
        return jsonResponse([])
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

      if (url.endsWith('/api/clones') || url.endsWith('/api/voices')) {
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

  it('streams a chat reply through the live session flow', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('ssarvis.access-token', 'chat-token')

    vi.spyOn(window, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)

      if (url.endsWith('/questions.json')) {
        return jsonResponse([{ question: 'Q1', choices: ['A', 'B'] }])
      }

      if (url.endsWith('/api/clones')) {
        return jsonResponse([
          {
            cloneId: 101,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: 'Alpha',
            shortDescription: '대화를 시작할 클론',
          },
        ])
      }

      if (url.endsWith('/api/voices')) {
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

      if (url.endsWith('/api/clones')) {
        return Promise.resolve(jsonResponse([
          {
            cloneId: 101,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: 'Alpha',
            shortDescription: '첫 번째 토론 클론',
          },
          {
            cloneId: 202,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: 'Beta',
            shortDescription: '두 번째 토론 클론',
          },
        ]))
      }

      if (url.endsWith('/api/voices')) {
        return Promise.resolve(jsonResponse([
          {
            registeredVoiceId: 1,
            voiceId: 'voice-a',
            displayName: '목소리 A',
            preferredName: 'voicea',
            originalFilename: 'a.wav',
            audioMimeType: 'audio/wav',
          },
          {
            registeredVoiceId: 2,
            voiceId: 'voice-b',
            displayName: '목소리 B',
            preferredName: 'voiceb',
            originalFilename: 'b.wav',
            audioMimeType: 'audio/wav',
          },
        ]))
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

      if (url.endsWith('/api/clones')) {
        return jsonResponse([
          {
            cloneId: 101,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: 'Alpha',
            shortDescription: '첫 번째 클론',
          },
          {
            cloneId: 202,
            createdAt: '2026-03-26T00:00:00.000Z',
            alias: 'Beta',
            shortDescription: '두 번째 클론',
          },
        ])
      }

      if (url.endsWith('/api/voices')) {
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
