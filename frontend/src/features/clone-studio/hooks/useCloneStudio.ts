import { useEffect, useMemo, useRef, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { apiBaseUrl, questionAssetPath, readErrorMessage, readNdjsonStream } from '../api'
import type {
  CloneOption,
  LiveChatState,
  LiveDebateState,
  ModalState,
  PromptGenerateResponse,
  Question,
  VoiceOption,
  VoiceRegisterResponse,
} from '../types'
import PcmStreamPlayer from '../../../utils/PcmStreamPlayer'
import { useSpeechInput } from './useSpeechInput'

export function useCloneStudio() {
  const [activeTab, setActiveTab] = useState<'clones' | 'live'>('clones')
  const [modalState, setModalState] = useState<ModalState>(null)
  const [questions, setQuestions] = useState<Question[]>([])
  const [answers, setAnswers] = useState<Record<number, string>>({})
  const [loadingQuestions, setLoadingQuestions] = useState(true)
  const [questionError, setQuestionError] = useState('')
  const [creatingClone, setCreatingClone] = useState(false)
  const [createCloneError, setCreateCloneError] = useState('')
  const [clones, setClones] = useState<CloneOption[]>([])
  const [cloneLoadError, setCloneLoadError] = useState('')
  const [voices, setVoices] = useState<VoiceOption[]>([])
  const [voiceLoadError, setVoiceLoadError] = useState('')
  const [selectedVoiceId, setSelectedVoiceId] = useState('')
  const [voiceAlias, setVoiceAlias] = useState('')
  const [voiceFile, setVoiceFile] = useState<File | null>(null)
  const [voiceRegistering, setVoiceRegistering] = useState(false)
  const [voiceRegisterError, setVoiceRegisterError] = useState('')
  const [debateTopic, setDebateTopic] = useState('')
  const [debateOpponentId, setDebateOpponentId] = useState('')
  const [debateVoiceAId, setDebateVoiceAId] = useState('')
  const [debateVoiceBId, setDebateVoiceBId] = useState('')
  const [debateSetupError, setDebateSetupError] = useState('')
  const [liveChat, setLiveChat] = useState<LiveChatState | null>(null)
  const [liveDebate, setLiveDebate] = useState<LiveDebateState | null>(null)

  const chatAbortControllerRef = useRef<AbortController | null>(null)
  const debateAbortControllerRef = useRef<AbortController | null>(null)
  const debateSessionIdRef = useRef<number | null>(null)
  const debateRunningRef = useRef(false)
  const liveChatInputRef = useRef('')

  const speechInput = useSpeechInput({
    getInput: () => liveChatInputRef.current,
    onInputChange: (value) => {
      setLiveChat((current) =>
        current
          ? {
              ...current,
              input: value,
            }
          : current
      )
    },
  })

  const selectedClone =
    modalState?.type === 'clone-actions' || modalState?.type === 'voice-picker' || modalState?.type === 'debate-setup'
      ? modalState.clone
      : null

  const answeredCount = useMemo(
    () => Object.values(answers).filter((answer) => answer.trim().length > 0).length,
    [answers]
  )

  const canCreateClone = questions.length > 0 && answeredCount === questions.length
  const debateOpponent = useMemo(
    () => clones.find((clone) => clone.cloneId === Number(debateOpponentId)) ?? null,
    [clones, debateOpponentId]
  )

  useEffect(() => {
    const controller = new AbortController()

    async function loadQuestions() {
      setLoadingQuestions(true)
      setQuestionError('')

      try {
        const response = await fetch(questionAssetPath, { signal: controller.signal })
        if (!response.ok) {
          throw new Error(`질문 목록을 불러오지 못했습니다. (${response.status})`)
        }
        const data: Question[] = await response.json()
        setQuestions(data)
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return
        }
        setQuestionError(error instanceof Error ? error.message : '질문 목록을 불러오는 중 오류가 발생했습니다.')
      } finally {
        setLoadingQuestions(false)
      }
    }

    void loadQuestions()
    void loadClones()
    void loadVoices()

    return () => controller.abort()
  }, [])

  useEffect(() => {
    debateSessionIdRef.current = liveDebate?.debateSessionId ?? null
    debateRunningRef.current = liveDebate?.running ?? false
  }, [liveDebate])

  useEffect(() => {
    liveChatInputRef.current = liveChat?.input ?? ''
  }, [liveChat?.input])

  useEffect(() => {
    if (activeTab !== 'live' && liveDebate?.running) {
      void handleDebateStop()
    }
  }, [activeTab, liveDebate?.running])

  useEffect(() => {
    setLiveChat((current) =>
      current
        ? {
            ...current,
            speechSupported: speechInput.supported,
            speechListening: speechInput.listening,
            speechError: speechInput.error,
          }
        : current
    )
  }, [speechInput.supported, speechInput.listening, speechInput.error])

  useEffect(() => {
    return () => {
      chatAbortControllerRef.current?.abort()
      debateAbortControllerRef.current?.abort()
      void speechInput.stop()
      const debateSessionId = debateSessionIdRef.current
      if (debateSessionId) {
        void fetch(`${apiBaseUrl}/api/debates/${debateSessionId}/stop`, {
          method: 'POST',
          keepalive: true,
        })
      }
    }
  }, [])

  async function loadClones() {
    try {
      setCloneLoadError('')
      const response = await fetch(`${apiBaseUrl}/api/clones`)
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `클론 목록을 불러오지 못했습니다. (${response.status})`))
      }
      const data: CloneOption[] = await response.json()
      setClones(data)
    } catch (error) {
      setCloneLoadError(error instanceof Error ? error.message : '클론 목록을 불러오는 중 오류가 발생했습니다.')
    }
  }

  async function loadVoices() {
    try {
      setVoiceLoadError('')
      const response = await fetch(`${apiBaseUrl}/api/voices`)
      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `목소리 목록을 불러오지 못했습니다. (${response.status})`))
      }
      const data: VoiceOption[] = await response.json()
      setVoices(data)
    } catch (error) {
      setVoiceLoadError(error instanceof Error ? error.message : '목소리 목록을 불러오는 중 오류가 발생했습니다.')
    }
  }

  function resetCreateCloneFlow() {
    setAnswers({})
    setCreateCloneError('')
  }

  function resetVoicePickerState() {
    setSelectedVoiceId('')
    setVoiceAlias('')
    setVoiceFile(null)
    setVoiceRegisterError('')
  }

  function resetDebateSetupState() {
    setDebateTopic('')
    setDebateOpponentId('')
    setDebateVoiceAId('')
    setDebateVoiceBId('')
    setDebateSetupError('')
  }

  function closeModal() {
    setModalState(null)
    resetCreateCloneFlow()
    resetVoicePickerState()
    resetDebateSetupState()
  }

  function openCreateCloneModal() {
    resetCreateCloneFlow()
    setModalState({ type: 'create-clone' })
  }

  function openCloneActions(clone: CloneOption) {
    resetVoicePickerState()
    resetDebateSetupState()
    setModalState({ type: 'clone-actions', clone })
  }

  function openVoicePicker() {
    if (selectedClone) {
      setModalState({ type: 'voice-picker', clone: selectedClone })
    }
  }

  function openDebateSetup() {
    if (selectedClone) {
      setModalState({ type: 'debate-setup', clone: selectedClone })
    }
  }

  function goBackToCloneActions() {
    if (selectedClone) {
      setModalState({ type: 'clone-actions', clone: selectedClone })
    }
  }

  function handleQuestionAnswer(questionIndex: number, answer: string) {
    setAnswers((current) => ({
      ...current,
      [questionIndex]: answer,
    }))
  }

  async function handleCloneCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!canCreateClone) {
      setCreateCloneError('모든 질문에 답변해야 새 클론을 만들 수 있습니다.')
      return
    }

    setCreatingClone(true)
    setCreateCloneError('')

    try {
      const response = await fetch(`${apiBaseUrl}/api/system-prompt`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          answers: questions.map((question, index) => ({
            question: question.question,
            answer: answers[index],
          })),
        }),
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `클론 생성에 실패했습니다. (${response.status})`))
      }

      const data: PromptGenerateResponse = await response.json()
      const nextClone: CloneOption = {
        cloneId: data.promptGenerationLogId,
        createdAt: new Date().toISOString(),
        alias: data.alias,
        shortDescription: data.shortDescription,
      }

      setClones((current) => [nextClone, ...current.filter((clone) => clone.cloneId !== nextClone.cloneId)])
      closeModal()
    } catch (error) {
      setCreateCloneError(error instanceof Error ? error.message : '클론 생성 중 오류가 발생했습니다.')
    } finally {
      setCreatingClone(false)
    }
  }

  async function handleVoiceRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!voiceFile) {
      setVoiceRegisterError('업로드할 목소리 샘플 파일을 선택해 주세요.')
      return
    }

    setVoiceRegistering(true)
    setVoiceRegisterError('')

    try {
      const formData = new FormData()
      formData.append('sample', voiceFile)
      if (voiceAlias.trim()) {
        formData.append('alias', voiceAlias.trim())
      }

      const response = await fetch(`${apiBaseUrl}/api/voices`, {
        method: 'POST',
        body: formData,
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `목소리 등록에 실패했습니다. (${response.status})`))
      }

      const data: VoiceRegisterResponse = await response.json()
      const nextVoice: VoiceOption = {
        registeredVoiceId: data.registeredVoiceId,
        voiceId: data.voiceId,
        displayName: data.displayName,
        preferredName: data.preferredName,
        originalFilename: data.originalFilename,
        audioMimeType: data.audioMimeType,
        createdAt: new Date().toISOString(),
      }

      setVoices((current) => [nextVoice, ...current.filter((voice) => voice.registeredVoiceId !== nextVoice.registeredVoiceId)])
      setSelectedVoiceId(String(nextVoice.registeredVoiceId))
      setDebateVoiceAId(String(nextVoice.registeredVoiceId))
      setDebateVoiceBId(String(nextVoice.registeredVoiceId))
      setVoiceAlias('')
      setVoiceFile(null)
    } catch (error) {
      setVoiceRegisterError(error instanceof Error ? error.message : '목소리 등록 중 오류가 발생했습니다.')
    } finally {
      setVoiceRegistering(false)
    }
  }

  async function streamChatMessage(message: string) {
    if (!liveChat) {
      return
    }

    await speechInput.stop()

    const player = new PcmStreamPlayer()
    const abortController = new AbortController()
    chatAbortControllerRef.current = abortController

    setLiveChat((current) =>
      current
        ? {
            ...current,
            submitting: true,
            error: '',
            input: '',
            messages: [...current.messages, { role: 'user', content: message }],
          }
        : current
    )

    try {
      const requestBody = {
        promptGenerationLogId: liveChat.clone.cloneId,
        conversationId: liveChat.conversationId,
        message,
        ...(liveChat.voiceId ? { registeredVoiceId: liveChat.voiceId } : {}),
      }

      const response = await fetch(`${apiBaseUrl}/api/chat/messages/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody),
        signal: abortController.signal,
      })

      if (!response.ok) {
        throw new Error(await readErrorMessage(response, `대화 시작에 실패했습니다. (${response.status})`))
      }

      let pendingVoiceId: string | undefined

      await readNdjsonStream(response, async (streamEvent) => {
        if (streamEvent.type === 'message') {
          setLiveChat((current) =>
            current
              ? {
                  ...current,
                  conversationId: streamEvent.conversationId,
                  messages: [...current.messages, { role: 'assistant', content: streamEvent.assistantMessage }],
                }
              : current
          )
          return
        }

        if (streamEvent.type === 'audio_chunk') {
          player.configure(streamEvent.sampleRate, streamEvent.channels)
          await player.appendBase64Chunk(streamEvent.chunkBase64)
          return
        }

        if (streamEvent.type === 'done') {
          pendingVoiceId = streamEvent.ttsVoiceId || undefined
          await player.finish()
          const wavUrl = player.buildWavUrl()
          setLiveChat((current) => {
            if (!current) {
              return current
            }
            const nextMessages = [...current.messages]
            for (let index = nextMessages.length - 1; index >= 0; index -= 1) {
              if (nextMessages[index].role === 'assistant' && !nextMessages[index].ttsAudioDataUrl) {
                nextMessages[index] = {
                  ...nextMessages[index],
                  ttsAudioDataUrl: wavUrl,
                  ttsVoiceId: pendingVoiceId,
                }
                break
              }
            }
            return {
              ...current,
              messages: nextMessages,
            }
          })
          return
        }

        if (streamEvent.type === 'error') {
          throw new Error(streamEvent.message)
        }
      })
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      setLiveChat((current) =>
        current
          ? {
              ...current,
              error: error instanceof Error ? error.message : '대화 중 오류가 발생했습니다.',
            }
          : current
      )
    } finally {
      await player.dispose()
      chatAbortControllerRef.current = null
      setLiveChat((current) =>
        current
          ? {
              ...current,
              submitting: false,
            }
          : current
      )
    }
  }

  async function streamDebateTurn(url: string, body?: object) {
    const player = new PcmStreamPlayer()
    const abortController = new AbortController()
    debateAbortControllerRef.current = abortController

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

      let currentTurnIndex: number | null = null
      let currentVoiceId: string | undefined

      await readNdjsonStream(response, async (streamEvent) => {
        if (streamEvent.type === 'turn') {
          currentTurnIndex = streamEvent.turn.turnIndex
          setLiveDebate((current) =>
            current
              ? {
                  ...current,
                  debateSessionId: streamEvent.debateSessionId,
                  turns: [...current.turns, { ...streamEvent.turn }],
                }
              : current
          )
          return
        }

        if (streamEvent.type === 'audio_chunk') {
          player.configure(streamEvent.sampleRate, streamEvent.channels)
          await player.appendBase64Chunk(streamEvent.chunkBase64)
          return
        }

        if (streamEvent.type === 'done') {
          currentVoiceId = streamEvent.ttsVoiceId || undefined
          await player.finish()
          const wavUrl = player.buildWavUrl()
          setLiveDebate((current) =>
            current
              ? {
                  ...current,
                  turns: current.turns.map((turn) =>
                    turn.turnIndex === currentTurnIndex
                      ? { ...turn, ttsAudioDataUrl: wavUrl, ttsVoiceId: currentVoiceId }
                      : turn
                  ),
                }
              : current
          )
          return
        }

        if (streamEvent.type === 'error') {
          throw new Error(streamEvent.message)
        }
      })
    } finally {
      debateAbortControllerRef.current = null
      await player.dispose()
    }
  }

  async function continueDebateLoop() {
    while (debateRunningRef.current && debateSessionIdRef.current) {
      try {
        await streamDebateTurn(`${apiBaseUrl}/api/debates/${debateSessionIdRef.current}/next/stream`)
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') {
          return
        }
        setLiveDebate((current) =>
          current
            ? {
                ...current,
                running: false,
                error: error instanceof Error ? error.message : '다음 논쟁 턴을 불러오지 못했습니다.',
              }
            : current
        )
        return
      }
    }
  }

  async function handleDebateStop() {
    const sessionId = debateSessionIdRef.current
    setLiveDebate((current) =>
      current
        ? {
            ...current,
            running: false,
            stopping: true,
          }
        : current
    )
    debateAbortControllerRef.current?.abort()

    try {
      if (sessionId) {
        await fetch(`${apiBaseUrl}/api/debates/${sessionId}/stop`, {
          method: 'POST',
        })
      }
    } catch {
      // Ignore stop failures.
    } finally {
      setLiveDebate((current) =>
        current
          ? {
              ...current,
              stopping: false,
            }
          : current
      )
    }
  }

  function handleVoiceFileChange(event: ChangeEvent<HTMLInputElement>) {
    setVoiceFile(event.target.files?.[0] ?? null)
  }

  async function handleStartChat() {
    if (!selectedClone) {
      return
    }
    setLiveDebate(null)
    setLiveChat({
      clone: selectedClone,
      voiceId: selectedVoiceId ? Number(selectedVoiceId) : null,
      conversationId: null,
      messages: [],
      input: '',
      submitting: false,
      error: '',
      speechSupported: speechInput.supported,
      speechListening: false,
      speechError: '',
    })
    setActiveTab('live')
    closeModal()
  }

  async function handleChatSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!liveChat || !liveChat.input.trim() || liveChat.submitting) {
      return
    }
    await streamChatMessage(liveChat.input.trim())
  }

  function handleChatInputChange(value: string) {
    speechInput.clearError()
    setLiveChat((current) =>
      current
        ? {
            ...current,
            input: value,
            speechError: '',
          }
        : current
    )
  }

  function handleChatSpeechToggle() {
    void speechInput.toggle()
  }

  async function handleStartDebate() {
    if (!selectedClone) {
      return
    }
    if (!debateOpponentId || Number(debateOpponentId) === selectedClone.cloneId) {
      setDebateSetupError('논쟁시킬 두 번째 클론을 선택해 주세요.')
      return
    }
    if (!debateVoiceAId || !debateVoiceBId) {
      setDebateSetupError('두 클론의 목소리를 모두 선택해 주세요.')
      return
    }
    if (!debateTopic.trim()) {
      setDebateSetupError('논쟁 주제를 입력해 주세요.')
      return
    }

    const cloneB = clones.find((clone) => clone.cloneId === Number(debateOpponentId))
    if (!cloneB) {
      setDebateSetupError('선택한 상대 클론을 찾을 수 없습니다.')
      return
    }

    const nextState: LiveDebateState = {
      cloneA: selectedClone,
      cloneB,
      cloneAVoiceId: Number(debateVoiceAId),
      cloneBVoiceId: Number(debateVoiceBId),
      topic: debateTopic.trim(),
      debateSessionId: null,
      turns: [],
      running: true,
      stopping: false,
      error: '',
    }

    await speechInput.stop()
    setLiveChat(null)
    setLiveDebate(nextState)
    setActiveTab('live')
    closeModal()

    try {
      await streamDebateTurn(`${apiBaseUrl}/api/debates/stream`, {
        cloneAId: selectedClone.cloneId,
        cloneBId: cloneB.cloneId,
        cloneAVoiceId: Number(debateVoiceAId),
        cloneBVoiceId: Number(debateVoiceBId),
        topic: debateTopic.trim(),
      })
      if (debateRunningRef.current) {
        void continueDebateLoop()
      }
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return
      }
      setLiveDebate((current) =>
        current
          ? {
              ...current,
              running: false,
              error: error instanceof Error ? error.message : '논쟁을 시작하지 못했습니다.',
            }
          : current
      )
    }
  }

  return {
    activeTab,
    setActiveTab,
    modalState,
    selectedClone,
    questions,
    answers,
    loadingQuestions,
    questionError,
    creatingClone,
    createCloneError,
    clones,
    cloneLoadError,
    voices,
    voiceLoadError,
    selectedVoiceId,
    setSelectedVoiceId,
    voiceAlias,
    setVoiceAlias,
    voiceRegistering,
    voiceRegisterError,
    debateTopic,
    setDebateTopic,
    debateOpponentId,
    setDebateOpponentId,
    debateOpponent,
    debateVoiceAId,
    setDebateVoiceAId,
    debateVoiceBId,
    setDebateVoiceBId,
    debateSetupError,
    liveChat,
    liveDebate,
    answeredCount,
    canCreateClone,
    closeModal,
    openCreateCloneModal,
    openCloneActions,
    openVoicePicker,
    openDebateSetup,
    goBackToCloneActions,
    handleQuestionAnswer,
    handleCloneCreate,
    handleVoiceRegister,
    handleVoiceFileChange,
    handleStartChat,
    handleChatSubmit,
    handleChatInputChange,
    handleChatSpeechToggle,
    handleStartDebate,
    handleDebateStop,
  }
}
