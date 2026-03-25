import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import QuestionnairePanel from '../components/QuestionnairePanel'
import type { Question } from '../components/QuestionnairePanel'
import ResultPanel from '../components/ResultPanel'
import type { ChatMessage } from '../components/ResultPanel'
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

type ChatResponse = {
  conversationId: number
  assistantMessage: string
  ttsVoiceId?: string | null
  ttsAudioMimeType?: string | null
  ttsAudioBase64?: string | null
}

type VoiceRegisterResponse = {
  registeredVoiceId: number
  voiceId: string
  preferredName: string
  originalFilename: string
  audioMimeType: string
}

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

  function buildAudioDataUrl(mimeType?: string | null, base64?: string | null) {
    if (!mimeType || !base64) {
      return undefined
    }
    return `data:${mimeType};base64,${base64}`
  }

  async function autoplayAudio(dataUrl?: string) {
    if (!dataUrl) {
      return
    }

    try {
      const audio = new Audio(dataUrl)
      await audio.play()
    } catch {
      // Autoplay may be blocked by the browser. The audio controls remain available for manual playback.
    }
  }

  useEffect(() => {
    const controller = new AbortController()

    async function loadQuestions() {
      setLoadingQuestions(true)
      setError('')

      try {
        const response = await fetch(questionAssetPath, {
          signal: controller.signal,
        })

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

    return () => controller.abort()
  }, [])

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
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      })

      if (!response.ok) {
        const message = await readErrorMessage(response, `시스템 프롬프트 생성에 실패했습니다. (${response.status})`)
        throw new Error(message)
      }

      const data: PromptGenerateResponse = await response.json()
      setPromptGenerationLogId(data.promptGenerationLogId)
      setConversationId(null)
      setChatMessages([])
      setChatInput('')
      setChatError('')
      setVoiceRegisterError('')
      setSystemPrompt(data.systemPrompt)
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
        const message = await readErrorMessage(response, `음성 등록에 실패했습니다. (${response.status})`)
        throw new Error(message)
      }

      const data: VoiceRegisterResponse = await response.json()
      setRegisteredVoiceId(data.registeredVoiceId)
      setRegisteredVoiceLabel(`${data.originalFilename} -> ${data.voiceId}`)
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

    setChatSubmitting(true)
    setChatError('')

    try {
      const response = await fetch(`${apiBaseUrl}/api/chat/messages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          promptGenerationLogId,
          conversationId,
          registeredVoiceId,
          message,
        }),
      })

      if (!response.ok) {
        const errorMessage = await readErrorMessage(response, `채팅 응답 생성에 실패했습니다. (${response.status})`)
        throw new Error(errorMessage)
      }

      const data: ChatResponse = await response.json()
      const ttsAudioDataUrl = buildAudioDataUrl(data.ttsAudioMimeType, data.ttsAudioBase64)
      setConversationId(data.conversationId)
      setChatMessages((current) => [
        ...current,
        { role: 'user', content: message },
        {
          role: 'assistant',
          content: data.assistantMessage,
          ttsAudioDataUrl,
          ttsVoiceId: data.ttsVoiceId ?? undefined,
        },
      ])
      setChatInput('')
      void autoplayAudio(ttsAudioDataUrl)
    } catch (submitError) {
      setChatError(submitError instanceof Error ? submitError.message : '채팅 중 오류가 발생했습니다.')
    } finally {
      setChatSubmitting(false)
    }
  }

  return (
    <main className="app-shell">
      <section className="hero-panel">
        <p className="eyebrow">Prompt Builder</p>
        <h1>응답 기반 시스템 프롬프트 생성기</h1>
        <p className="hero-copy">
          질문에 답변하면 백엔드가 OpenAI를 호출해 성향과 대화 스타일을 반영한 시스템 프롬프트를 생성합니다.
        </p>
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
    </main>
  )
}

export default PromptBuilderPage
