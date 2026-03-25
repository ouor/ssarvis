import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import QuestionnairePanel from '../components/QuestionnairePanel'
import type { Question } from '../components/QuestionnairePanel'
import ResultPanel from '../components/ResultPanel'
import type { ChatMessage } from '../components/ResultPanel'
import DebatePanel from '../components/DebatePanel'
import type { CloneOption, DebateTurn, VoiceOption } from '../components/DebatePanel'
import PcmStreamPlayer from '../utils/PcmStreamPlayer'
import './PromptBuilderPage.css'

type AnswerItem = {
  question: string
  answer: string
}

type ApiErrorResponse = {
  message?: string
  details?: string[]
}

type PromptGenerateResponse = {
  promptGenerationLogId: number
  systemPrompt: string
}

type VoiceRegisterResponse = {
  registeredVoiceId: number
  voiceId: string
  preferredName: string
  originalFilename: string
  audioMimeType: string
}

type StreamEvent =
  | { type: 'message'; conversationId: number; assistantMessage: string }
  | { type: 'turn'; debateSessionId: number; topic: string; turn: { turnIndex: number; speaker: string; cloneId: number; content: string } }
  | { type: 'audio_chunk'; audioFormat: string; sampleRate: number; channels: number; chunkBase64: string }
  | { type: 'done'; conversationId?: number; debateSessionId?: number; turnIndex?: number; ttsVoiceId?: string; hasAudio?: boolean }
  | { type: 'error'; message: string }

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
const questionAssetPath = `${import.meta.env.BASE_URL}questions.json`

function PromptBuilderPage() {
  const [questions, setQuestions] = useState<Question[]>([])
  const [answers, setAnswers] = useState<Record<number, string>>({})
  const [loadingQuestions, setLoadingQuestions] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [systemPrompt, setSystemPrompt] = useState('')
  const [error, setError] = useState('')
  const [promptGenerationLogId, setPromptGenerationLogId] = useState<number | null>(null)
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([])
  const [chatInput, setChatInput] = useState('')
  const [chatSubmitting, setChatSubmitting] = useState(false)
  const [chatError, setChatError] = useState('')
  const [voiceFile, setVoiceFile] = useState<File | null>(null)
  const [registeredVoiceId, setRegisteredVoiceId] = useState<number | null>(null)
  const [registeredVoiceLabel, setRegisteredVoiceLabel] = useState('')
  const [voiceRegistering, setVoiceRegistering] = useState(false)
  const [voiceRegisterError, setVoiceRegisterError] = useState('')
  const [clones, setClones] = useState<CloneOption[]>([])
  const [voices, setVoices] = useState<VoiceOption[]>([])
  const [cloneAId, setCloneAId] = useState('')
  const [cloneBId, setCloneBId] = useState('')
  const [cloneAVoiceId, setCloneAVoiceId] = useState('')
  const [cloneBVoiceId, setCloneBVoiceId] = useState('')
  const [debateTopic, setDebateTopic] = useState('')
  const [debateSessionId, setDebateSessionId] = useState<number | null>(null)
  const [debateRunning, setDebateRunning] = useState(false)
  const [debateStopping, setDebateStopping] = useState(false)
  const [debateTurnsList, setDebateTurnsList] = useState<DebateTurn[]>([])
  const [debateSubmitting, setDebateSubmitting] = useState(false)
  const [debateError, setDebateError] = useState('')

  const debateSessionIdRef = useRef<number | null>(null)
  const debateRunningRef = useRef(false)
  const chatAbortControllerRef = useRef<AbortController | null>(null)
  const debateAbortControllerRef = useRef<AbortController | null>(null)
  const debatePlayerRef = useRef<PcmStreamPlayer | null>(null)

  useEffect(() => {
    const controller = new AbortController()

    async function loadQuestions() {
      setLoadingQuestions(true)
      setError('')

      try {
        const response = await fetch(questionAssetPath, { signal: controller.signal })
        if (!response.ok) {
          throw new Error(`질문 목록을 불러오지 못했습니다. (${response.status})`)
        }

        const data: Question[] = await response.json()
        setQuestions(data)
      } catch (fetchError) {
        if (fetchError instanceof DOMException && fetchError.name === 'AbortError') {
          return
        }
        setError(fetchError instanceof Error ? fetchError.message : '질문 목록을 불러오는 중 오류가 발생했습니다.')
      } finally {
        setLoadingQuestions(false)
      }
    }

    loadQuestions()
    void loadClones()
    void loadVoices()

    return () => controller.abort()
  }, [])

  useEffect(() => {
    debateSessionIdRef.current = debateSessionId
  }, [debateSessionId])

  useEffect(() => {
    debateRunningRef.current = debateRunning
  }, [debateRunning])

  useEffect(() => {
    return () => {
      chatAbortControllerRef.current?.abort()
      debateAbortControllerRef.current?.abort()
      void debatePlayerRef.current?.dispose()
      const activeSessionId = debateSessionIdRef.current
      if (activeSessionId) {
        void stopDebateSession(activeSessionId, true)
      }
    }
  }, [])

  async function loadClones() {
    try {
      const response = await fetch(`${apiBaseUrl}/api/clones`)
      if (!response.ok) {
        return
      }
      const data: CloneOption[] = await response.json()
      setClones(data)
    } catch {
      // Ignore background clone refresh failures.
    }
  }

  async function loadVoices() {
    try {
      const response = await fetch(`${apiBaseUrl}/api/voices`)
      if (!response.ok) {
        return
      }
      const data: VoiceOption[] = await response.json()
      setVoices(data)
    } catch {
      // Ignore background voice refresh failures.
    }
  }

  const answeredCount = Object.values(answers).filter((answer) => answer.trim().length > 0).length
  const isComplete = questions.length > 0 && answeredCount === questions.length

  function updateAnswer(questionIndex: number, answer: string) {
    setAnswers((current) => ({
      ...current,
      [questionIndex]: answer,
    }))
  }

  async function readErrorMessage(response: Response, fallbackMessage: string) {
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

  async function readNdjsonStream(response: Response, onEvent: (event: StreamEvent) => Promise<void> | void) {
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

  async function handlePromptSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!isComplete) {
      setError('모든 질문에 답변해야 시스템 프롬프트를 생성할 수 있습니다.')
      return
    }

    const payload: { answers: AnswerItem[] } = {
      answers: questions.map((question, index) => ({
        question: question.question,
        answer: answers[index],
      })),
    }

    setSubmitting(true)
    setError('')

    try {
      const response = await fetch(`${apiBaseUrl}/api/system-prompt`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `시스템 프롬프트 생성에 실패했습니다. (${response.status})`))
      }

      const data: PromptGenerateResponse = await response.json()
      setPromptGenerationLogId(data.promptGenerationLogId)
      setConversationId(null)
      setChatMessages([])
      setChatInput('')
      setChatError('')
      setVoiceRegisterError('')
      setSystemPrompt(data.systemPrompt)
      void loadClones()
    } catch (submitError) {
      setPromptGenerationLogId(null)
      setConversationId(null)
      setChatMessages([])
      setSystemPrompt('')
      setError(submitError instanceof Error ? submitError.message : '시스템 프롬프트 생성 중 오류가 발생했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleVoiceRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!voiceFile) {
      setVoiceRegisterError('등록할 음성 파일을 선택해 주세요.')
      return
    }

    setVoiceRegistering(true)
    setVoiceRegisterError('')

    try {
      const formData = new FormData()
      formData.append('sample', voiceFile)

      const response = await fetch(`${apiBaseUrl}/api/voices`, {
        method: 'POST',
        body: formData,
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `음성 등록에 실패했습니다. (${response.status})`))
      }

      const data: VoiceRegisterResponse = await response.json()
      setRegisteredVoiceId(data.registeredVoiceId)
      setRegisteredVoiceLabel(`${data.originalFilename} -> ${data.voiceId}`)
      setCloneAVoiceId(String(data.registeredVoiceId))
      setCloneBVoiceId(String(data.registeredVoiceId))
      void loadVoices()
    } catch (registerError) {
      setRegisteredVoiceId(null)
      setRegisteredVoiceLabel('')
      setVoiceRegisterError(registerError instanceof Error ? registerError.message : '음성 등록 중 오류가 발생했습니다.')
    } finally {
      setVoiceRegistering(false)
    }
  }

  async function handleChatSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!promptGenerationLogId) {
      setChatError('먼저 시스템 프롬프트를 생성해야 채팅을 시작할 수 있습니다.')
      return
    }

    if (!chatInput.trim()) {
      setChatError('메시지를 입력해 주세요.')
      return
    }

    const message = chatInput.trim()
    const abortController = new AbortController()
    const player = new PcmStreamPlayer()
    chatAbortControllerRef.current = abortController

    setChatSubmitting(true)
    setChatError('')
    setChatMessages((current) => [...current, { role: 'user', content: message }])
    setChatInput('')

    try {
      const response = await fetch(`${apiBaseUrl}/api/chat/messages/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          promptGenerationLogId,
          conversationId,
          registeredVoiceId,
          message,
        }),
        signal: abortController.signal,
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `채팅 스트림 생성에 실패했습니다. (${response.status})`))
      }

      let pendingVoiceId: string | undefined

      await readNdjsonStream(response, async (streamEvent) => {
        if (streamEvent.type === 'message') {
          setConversationId(streamEvent.conversationId)
          setChatMessages((current) => [...current, { role: 'assistant', content: streamEvent.assistantMessage }])
          return
        }

        if (streamEvent.type === 'audio_chunk') {
          await player.appendBase64Chunk(streamEvent.chunkBase64)
          return
        }

        if (streamEvent.type === 'done') {
          pendingVoiceId = streamEvent.ttsVoiceId || undefined
          await player.finish()
          const wavUrl = player.buildWavUrl()
          setChatMessages((current) => {
            const next = [...current]
            for (let index = next.length - 1; index >= 0; index -= 1) {
              if (next[index].role === 'assistant' && !next[index].ttsAudioDataUrl) {
                next[index] = {
                  ...next[index],
                  ttsAudioDataUrl: wavUrl,
                  ttsVoiceId: pendingVoiceId,
                }
                break
              }
            }
            return next
          })
          return
        }

        if (streamEvent.type === 'error') {
          throw new Error(streamEvent.message)
        }
      })
    } catch (submitError) {
      if (submitError instanceof DOMException && submitError.name === 'AbortError') {
        return
      }
      setChatError(submitError instanceof Error ? submitError.message : '채팅 중 오류가 발생했습니다.')
    } finally {
      await player.dispose()
      chatAbortControllerRef.current = null
      setChatSubmitting(false)
    }
  }

  function mapDebateTurn(turn: { turnIndex: number; speaker: string; cloneId: number; content: string }, ttsAudioDataUrl?: string, ttsVoiceId?: string): DebateTurn {
    return {
      turnIndex: turn.turnIndex,
      speaker: turn.speaker,
      cloneId: turn.cloneId,
      content: turn.content,
      ttsAudioDataUrl,
      ttsVoiceId,
    }
  }

  async function streamDebateTurn(url: string, body?: object) {
    const abortController = new AbortController()
    const player = new PcmStreamPlayer()
    debateAbortControllerRef.current = abortController
    debatePlayerRef.current = player

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: body ? { 'Content-Type': 'application/json' } : undefined,
        body: body ? JSON.stringify(body) : undefined,
        signal: abortController.signal,
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `논쟁 스트림 생성에 실패했습니다. (${response.status})`))
      }

      let currentTurn: DebateTurn | null = null
      let voiceId: string | undefined

      await readNdjsonStream(response, async (streamEvent) => {
        if (streamEvent.type === 'turn') {
          setDebateSessionId(streamEvent.debateSessionId)
          currentTurn = mapDebateTurn(streamEvent.turn)
          setDebateTurnsList((current) => [...current, currentTurn!])
          return
        }

        if (streamEvent.type === 'audio_chunk') {
          await player.appendBase64Chunk(streamEvent.chunkBase64)
          return
        }

        if (streamEvent.type === 'done') {
          voiceId = streamEvent.ttsVoiceId || undefined
          await player.finish()
          const wavUrl = player.buildWavUrl()
          if (currentTurn) {
            setDebateTurnsList((current) =>
              current.map((turn) =>
                turn.turnIndex === currentTurn!.turnIndex
                  ? { ...turn, ttsAudioDataUrl: wavUrl, ttsVoiceId: voiceId }
                  : turn
              )
            )
          }
          return
        }

        if (streamEvent.type === 'error') {
          throw new Error(streamEvent.message)
        }
      })
    } catch (streamError) {
      if (streamError instanceof DOMException && streamError.name === 'AbortError') {
        return
      }
      throw streamError
    } finally {
      debateAbortControllerRef.current = null
      debatePlayerRef.current = null
      await player.dispose()
    }
  }

  async function continueDebateLoop() {
    while (debateRunningRef.current && debateSessionIdRef.current) {
      try {
        await streamDebateTurn(`${apiBaseUrl}/api/debates/${debateSessionIdRef.current}/next/stream`)
      } catch (turnError) {
        if (turnError instanceof DOMException && turnError.name === 'AbortError') {
          return
        }
        setDebateRunning(false)
        setDebateError(turnError instanceof Error ? turnError.message : '다음 논쟁 턴 생성 중 오류가 발생했습니다.')
        return
      }
    }
  }

  async function handleDebateSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!cloneAId || !cloneBId) {
      setDebateError('논쟁할 두 클론을 모두 선택해 주세요.')
      return
    }
    if (!cloneAVoiceId || !cloneBVoiceId) {
      setDebateError('두 클론에 사용할 음성을 모두 선택해 주세요.')
      return
    }
    if (!debateTopic.trim()) {
      setDebateError('논쟁 주제를 입력해 주세요.')
      return
    }

    setDebateSubmitting(true)
    setDebateError('')
    setDebateTurnsList([])
    setDebateRunning(true)
    setDebateStopping(false)

    try {
      await streamDebateTurn(`${apiBaseUrl}/api/debates/stream`, {
        cloneAId: Number(cloneAId),
        cloneBId: Number(cloneBId),
        cloneAVoiceId: Number(cloneAVoiceId),
        cloneBVoiceId: Number(cloneBVoiceId),
        topic: debateTopic,
      })
      if (debateRunningRef.current) {
        void continueDebateLoop()
      }
    } catch (submitError) {
      if (submitError instanceof DOMException && submitError.name === 'AbortError') {
        return
      }
      setDebateRunning(false)
      setDebateError(submitError instanceof Error ? submitError.message : '논쟁 시작 중 오류가 발생했습니다.')
    } finally {
      setDebateSubmitting(false)
    }
  }

  async function stopDebateSession(sessionId: number, keepalive = false) {
    await fetch(`${apiBaseUrl}/api/debates/${sessionId}/stop`, {
      method: 'POST',
      keepalive,
    })
  }

  async function handleDebateStop() {
    setDebateStopping(true)
    setDebateRunning(false)
    debateAbortControllerRef.current?.abort()

    try {
      if (debateSessionIdRef.current) {
        await stopDebateSession(debateSessionIdRef.current)
      }
    } catch {
      // Ignore stop request failures.
    } finally {
      debateAbortControllerRef.current = null
      debatePlayerRef.current = null
      setDebateStopping(false)
    }
  }

  return (
    <main className="app-shell">
      <section className="hero-panel">
        <p className="eyebrow">Prompt Builder</p>
        <h1>응답 기반 시스템 프롬프트 생성기</h1>
        <p className="hero-copy">질문에 답변하면 백엔드가 OpenAI를 호출해 성향과 대화 스타일을 반영한 시스템 프롬프트를 생성합니다.</p>
        <div className="hero-meta">
          <span>{loadingQuestions ? '질문 로딩 중' : `질문 ${questions.length}개`}</span>
          <span>답변 {answeredCount}개</span>
        </div>
      </section>

      <section className="content-grid">
        <QuestionnairePanel
          answeredCount={answeredCount}
          answers={answers}
          loadingQuestions={loadingQuestions}
          onAnswerChange={updateAnswer}
          onSubmit={handlePromptSubmit}
          questions={questions}
          submitting={submitting}
        />
        <ResultPanel
          chatError={chatError}
          chatInput={chatInput}
          chatMessages={chatMessages}
          chatSubmitting={chatSubmitting}
          conversationId={conversationId}
          error={error}
          onVoiceFileChange={setVoiceFile}
          onChatInputChange={setChatInput}
          onChatSubmit={handleChatSubmit}
          onVoiceRegister={handleVoiceRegister}
          registeredVoiceLabel={registeredVoiceLabel}
          systemPrompt={systemPrompt}
          voiceRegisterError={voiceRegisterError}
          voiceRegistering={voiceRegistering}
        />
      </section>

      <DebatePanel
        cloneAId={cloneAId}
        cloneAVoiceId={cloneAVoiceId}
        cloneBId={cloneBId}
        cloneBVoiceId={cloneBVoiceId}
        clones={clones}
        debateError={debateError}
        debateRunning={debateRunning}
        debateSessionId={debateSessionId}
        debateStopping={debateStopping}
        debateSubmitting={debateSubmitting}
        debateTopic={debateTopic}
        debateTurnsList={debateTurnsList}
        onCloneAChange={setCloneAId}
        onCloneAVoiceChange={setCloneAVoiceId}
        onCloneBChange={setCloneBId}
        onCloneBVoiceChange={setCloneBVoiceId}
        onDebateStop={handleDebateStop}
        onDebateSubmit={handleDebateSubmit}
        onTopicChange={setDebateTopic}
        voices={voices}
      />
    </main>
  )
}

export default PromptBuilderPage
